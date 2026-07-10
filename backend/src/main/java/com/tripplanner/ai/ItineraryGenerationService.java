package com.tripplanner.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiItineraryPayload;
import com.tripplanner.cache.AiResponseCacheService;
import com.tripplanner.cache.CacheKeyGenerator;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 최초 생성(§5.3/5.4) 오케스트레이션: 캐시 조회 → 미스 시 프롬프트 렌더링 → AI 호출(재시도) →
 * 파싱/검증(재시도) → 캐시 upsert. Trip/ItineraryDay/ItineraryActivity 영속화는 trip 모듈 책임.
 */
@Service
public class ItineraryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerationService.class);

    private static final String SYSTEM_PROMPT =
            "너는 여행 일정 플래너 AI다. 반드시 유효한 JSON 객체 하나만 출력하고, "
                    + "설명 문장이나 마크다운 코드펜스(```)는 절대 포함하지 마라.";

    private static final int MAX_ATTEMPTS = 2;
    private static final int BASE_MAX_TOKENS = 800;
    private static final int TOKENS_PER_DAY = 500;
    private static final int BASE_TIMEOUT_SECONDS = 15;
    private static final int TIMEOUT_SECONDS_PER_DAY = 5;
    private static final int MAX_TIMEOUT_SECONDS = 90;

    private final PromptTemplateService promptTemplateService;
    private final InitialGenerationPromptRenderer promptRenderer;
    private final AnthropicClient anthropicClient;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final AiResponseCacheService cacheService;
    private final ObjectMapper objectMapper;

    public ItineraryGenerationService(
            PromptTemplateService promptTemplateService,
            InitialGenerationPromptRenderer promptRenderer,
            AnthropicClient anthropicClient,
            CacheKeyGenerator cacheKeyGenerator,
            AiResponseCacheService cacheService,
            ObjectMapper objectMapper) {
        this.promptTemplateService = promptTemplateService;
        this.promptRenderer = promptRenderer;
        this.anthropicClient = anthropicClient;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public AiItineraryPayload generate(
            String destination, int durationDays, String budgetLevel, List<String> preferences, boolean includeNearby) {
        String cacheKey = cacheKeyGenerator.generate(destination, durationDays, budgetLevel, preferences, includeNearby);

        Optional<AiItineraryPayload> cached = cacheService.lookup(cacheKey);
        if (cached.isPresent()) {
            log.info("AI 응답 캐시 히트, AI 미호출. cacheKey={}", cacheKey);
            return cached.get();
        }
        log.info("AI 응답 캐시 미스, AI 직접 호출 진행. cacheKey={}", cacheKey);

        PromptTemplate template = promptTemplateService.loadActive("initial_generation");
        String userPrompt = promptRenderer.render(template, destination, durationDays, budgetLevel, preferences, includeNearby);
        int maxTokens = BASE_MAX_TOKENS + TOKENS_PER_DAY * durationDays;
        Duration timeout = Duration.ofSeconds(
                Math.min(MAX_TIMEOUT_SECONDS, BASE_TIMEOUT_SECONDS + TIMEOUT_SECONDS_PER_DAY * durationDays));

        AiItineraryPayload payload = callAndParseWithRetry(userPrompt, maxTokens, timeout, durationDays);

        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("destination", destination);
        requestParams.put("duration_days", durationDays);
        requestParams.put("budget_level", budgetLevel);
        requestParams.put("preferences", preferences);
        requestParams.put("include_nearby", includeNearby);
        cacheService.upsert(cacheKey, requestParams, payload);

        return payload;
    }

    private AiItineraryPayload callAndParseWithRetry(
            String userPrompt, int maxTokens, Duration timeout, int durationDays) {
        AiCallException lastCallException = null;
        AiParseException lastParseException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw;
            try {
                raw = anthropicClient.complete(SYSTEM_PROMPT, userPrompt, maxTokens, timeout);
            } catch (AiCallException e) {
                lastCallException = e;
                log.warn("AI 호출 실패 (attempt {}/{})", attempt, MAX_ATTEMPTS, e);
                continue;
            }

            try {
                return parseAndValidate(raw, durationDays);
            } catch (AiParseException e) {
                lastParseException = e;
                log.warn("AI 응답 파싱/검증 실패 (attempt {}/{})", attempt, MAX_ATTEMPTS, e);
            }
        }

        if (lastParseException != null) {
            throw lastParseException;
        }
        throw lastCallException;
    }

    private AiItineraryPayload parseAndValidate(String raw, int durationDays) {
        String cleaned = stripMarkdownFence(raw);
        AiItineraryPayload payload;
        try {
            payload = objectMapper.readValue(cleaned, AiItineraryPayload.class);
        } catch (Exception e) {
            throw new AiParseException("AI 응답 JSON 파싱 실패", e);
        }

        if (payload.days() == null || payload.days().size() != durationDays) {
            throw new AiParseException(
                    "AI 응답 day 개수가 duration_days와 불일치: expected=" + durationDays
                            + " actual=" + (payload.days() == null ? 0 : payload.days().size()));
        }

        for (AiDayPayload day : payload.days()) {
            if (day.activities() == null || day.activities().isEmpty()) {
                throw new AiParseException("day " + day.day() + "에 activities가 없음");
            }
            boolean hasMissingCoordinates = day.activities().stream()
                    .anyMatch(a -> a.lat() == null || a.lng() == null || a.title() == null || a.title().isBlank());
            if (hasMissingCoordinates) {
                throw new AiParseException("day " + day.day() + "의 activity에 필수값(title/lat/lng) 누락");
            }
        }

        return payload;
    }

    private String stripMarkdownFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
