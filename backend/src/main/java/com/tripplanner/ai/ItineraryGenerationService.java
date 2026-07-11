package com.tripplanner.ai;

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
    private final AiItineraryPayloadParser payloadParser;

    public ItineraryGenerationService(
            PromptTemplateService promptTemplateService,
            InitialGenerationPromptRenderer promptRenderer,
            AnthropicClient anthropicClient,
            CacheKeyGenerator cacheKeyGenerator,
            AiResponseCacheService cacheService,
            AiItineraryPayloadParser payloadParser) {
        this.promptTemplateService = promptTemplateService;
        this.promptRenderer = promptRenderer;
        this.anthropicClient = anthropicClient;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.cacheService = cacheService;
        this.payloadParser = payloadParser;
    }

    public AiItineraryPayload generate(
            String destination,
            int durationDays,
            Integer budgetMin,
            Integer budgetMax,
            String companion,
            List<String> preferences,
            boolean includeNearby,
            String activeStartTime,
            String activeEndTime) {
        String cacheKey = cacheKeyGenerator.generate(
                destination,
                durationDays,
                budgetMin,
                budgetMax,
                companion,
                preferences,
                includeNearby,
                activeStartTime,
                activeEndTime);

        Optional<AiItineraryPayload> cached = cacheService.lookup(cacheKey);
        if (cached.isPresent()) {
            log.info("AI 응답 캐시 히트, AI 미호출. cacheKey={}", cacheKey);
            return cached.get();
        }
        log.info("AI 응답 캐시 미스, AI 직접 호출 진행. cacheKey={}", cacheKey);

        PromptTemplate template = promptTemplateService.loadActive("initial_generation");
        String userPrompt = promptRenderer.render(
                template,
                destination,
                durationDays,
                budgetMin,
                budgetMax,
                companion,
                preferences,
                includeNearby,
                activeStartTime,
                activeEndTime);
        int maxTokens = BASE_MAX_TOKENS + TOKENS_PER_DAY * durationDays;
        Duration timeout = Duration.ofSeconds(
                Math.min(MAX_TIMEOUT_SECONDS, BASE_TIMEOUT_SECONDS + TIMEOUT_SECONDS_PER_DAY * durationDays));

        AiItineraryPayload payload = callAndParseWithRetry(userPrompt, maxTokens, timeout, durationDays);

        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("destination", destination);
        requestParams.put("duration_days", durationDays);
        requestParams.put("budget_min", budgetMin);
        requestParams.put("budget_max", budgetMax);
        requestParams.put("companion", companion);
        requestParams.put("preferences", preferences);
        requestParams.put("include_nearby", includeNearby);
        requestParams.put("active_start_time", activeStartTime);
        requestParams.put("active_end_time", activeEndTime);
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
                return payloadParser.parse(raw, durationDays);
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
}
