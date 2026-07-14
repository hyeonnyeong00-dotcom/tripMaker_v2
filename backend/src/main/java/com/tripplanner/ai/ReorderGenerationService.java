package com.tripplanner.ai;

import com.tripplanner.ai.dto.AiActivityPayload;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiItineraryPayload;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CLAUDE.md 5.1 재조정 오케스트레이션: 원본 전체를 프롬프트에 담아 "변경 대상 day만 다시 계산하고
 * 나머지는 원본 그대로 반환"을 강제 → 응답을 {@link PartialRegenerationValidator}로 검증(위반 시 1회 재시도) →
 * 검증을 통과한 changedDay의 데이터만 반환한다(다른 날짜는 호출측이 DB 원본을 그대로 사용하므로 여기서 버린다).
 * 캐시는 적용하지 않는다(§5.3, 재조정 미적용).
 */
@Service
public class ReorderGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReorderGenerationService.class);

    private static final String SYSTEM_PROMPT =
            "너는 여행 일정 플래너 AI다. 반드시 유효한 JSON 객체 하나만 출력하고, "
                    + "설명 문장이나 마크다운 코드펜스(```)는 절대 포함하지 마라.";

    private static final int MAX_ATTEMPTS = 2;
    // 재조정도 전체 days를 다시 출력하므로 초기 생성과 동일한 실측 기준 예산 사용(잘림 방지).
    private static final int BASE_MAX_TOKENS = 1500;
    private static final int TOKENS_PER_DAY = 1600;
    private static final int BASE_TIMEOUT_SECONDS = 20;
    private static final int TIMEOUT_SECONDS_PER_DAY = 15;
    private static final int MAX_TIMEOUT_SECONDS = 180;

    private final PromptTemplateService promptTemplateService;
    private final ReorderPromptRenderer promptRenderer;
    private final AnthropicClient anthropicClient;
    private final AiItineraryPayloadParser payloadParser;
    private final PartialRegenerationValidator partialRegenerationValidator;

    public ReorderGenerationService(
            PromptTemplateService promptTemplateService,
            ReorderPromptRenderer promptRenderer,
            AnthropicClient anthropicClient,
            AiItineraryPayloadParser payloadParser,
            PartialRegenerationValidator partialRegenerationValidator) {
        this.promptTemplateService = promptTemplateService;
        this.promptRenderer = promptRenderer;
        this.anthropicClient = anthropicClient;
        this.payloadParser = payloadParser;
        this.partialRegenerationValidator = partialRegenerationValidator;
    }

    public AiDayPayload generate(AiItineraryPayload original, int changedDay, List<String> newActivityOrder) {
        PromptTemplate template = promptTemplateService.loadActive("reorder");
        String userPrompt = promptRenderer.render(template, original, changedDay, newActivityOrder);
        int durationDays = original.days().size();
        int maxTokens = BASE_MAX_TOKENS + TOKENS_PER_DAY * durationDays;
        Duration timeout = Duration.ofSeconds(
                Math.min(MAX_TIMEOUT_SECONDS, BASE_TIMEOUT_SECONDS + TIMEOUT_SECONDS_PER_DAY * durationDays));

        AiCallException lastCallException = null;
        AiParseException lastParseException = null;
        PartialRegenerationViolationException lastViolation = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw;
            try {
                raw = anthropicClient.complete(SYSTEM_PROMPT, userPrompt, maxTokens, timeout);
            } catch (AiCallException e) {
                lastCallException = e;
                log.warn("재조정 AI 호출 실패 (attempt {}/{})", attempt, MAX_ATTEMPTS, e);
                continue;
            }

            AiItineraryPayload candidate;
            try {
                candidate = payloadParser.parse(raw, durationDays);
            } catch (AiParseException e) {
                lastParseException = e;
                log.warn("재조정 AI 응답 파싱/검증 실패 (attempt {}/{})", attempt, MAX_ATTEMPTS, e);
                continue;
            }

            List<Integer> violatingDays =
                    partialRegenerationValidator.findViolatingDays(original.days(), candidate.days(), changedDay);
            if (!violatingDays.isEmpty()) {
                lastViolation = new PartialRegenerationViolationException(
                        "요청하지 않은 day가 원본과 다름: " + violatingDays);
                log.warn("부분 재생성 위반 (attempt {}/{}): violatingDays={}", attempt, MAX_ATTEMPTS, violatingDays);
                continue;
            }

            AiDayPayload changedDayPayload = candidate.days().stream()
                    .filter(d -> d.day() == changedDay)
                    .findFirst()
                    .orElse(null);
            if (changedDayPayload == null) {
                lastViolation = new PartialRegenerationViolationException("AI 응답에 요청한 day가 없음: day=" + changedDay);
                log.warn("재조정 응답에 대상 day 누락 (attempt {}/{}): day={}", attempt, MAX_ATTEMPTS, changedDay);
                continue;
            }

            List<String> returnedOrder =
                    changedDayPayload.activities().stream().map(AiActivityPayload::id).toList();
            if (!returnedOrder.equals(newActivityOrder)) {
                lastViolation = new PartialRegenerationViolationException(
                        "AI가 사용자가 정한 활동 순서를 임의로 변경함: expected=" + newActivityOrder + " actual=" + returnedOrder);
                log.warn("재조정 순서 임의 변경 감지 (attempt {}/{})", attempt, MAX_ATTEMPTS);
                continue;
            }

            return changedDayPayload;
        }

        if (lastViolation != null) {
            throw lastViolation;
        }
        if (lastParseException != null) {
            throw lastParseException;
        }
        throw lastCallException;
    }
}
