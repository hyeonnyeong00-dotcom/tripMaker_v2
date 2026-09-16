package com.tripplanner.ai;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 사용량 전용 로거. logback의 {@code ai-usage} named logger로 라우팅되어 {@code logs/ai-usage.log}에
 * 한 줄씩 적재된다(시각은 appender 패턴이 붙임). <b>보안: 프롬프트 본문/응답 본문/키는 절대 남기지 않는다.</b>
 * 템플릿은 이름+버전만, 그 외엔 토큰 수·소요 시간·성공 여부만 기록한다.
 * <p>§5.6-a에 따라 같은 정보를 {@code ai_call_log} 테이블에도 적재한다(대시보드는 파일을 파싱하지 않는다).
 * 모든 AI 시도(성공·실패·캐시 히트)가 이 클래스를 거치므로, DB 기록 지점도 여기 한 곳뿐이다.
 */
@Component
public class AiUsageLogger {

    private static final Logger usageLog = LoggerFactory.getLogger("ai-usage");
    private static final Logger log = LoggerFactory.getLogger(AiUsageLogger.class);

    private final AiCallLogRepository aiCallLogRepository;
    private final AiCostCalculator aiCostCalculator;

    public AiUsageLogger(AiCallLogRepository aiCallLogRepository, AiCostCalculator aiCostCalculator) {
        this.aiCallLogRepository = aiCallLogRepository;
        this.aiCostCalculator = aiCostCalculator;
    }

    /** 실제 AI 호출 1건(성공/실패)을 기록한다. 실패 시 errorCode를 남기고 토큰은 null 허용. */
    public void logCall(
            String tripId,
            String templateName,
            int templateVersion,
            Integer inputTokens,
            Integer outputTokens,
            long elapsedMs,
            boolean success,
            String errorCode) {
        usageLog.info("trip_id={} | template={} v{} | cache_hit=false | in_tokens={} out_tokens={} | elapsed_ms={} | {}",
                nullSafe(tripId),
                nullSafe(templateName),
                templateVersion,
                inputTokens == null ? "-" : inputTokens,
                outputTokens == null ? "-" : outputTokens,
                elapsedMs,
                success ? "success=true" : "success=false " + nullSafe(errorCode));

        persist(tripId, templateName, templateVersion, false,
                inputTokens, outputTokens, aiCostCalculator.estimate(inputTokens, outputTokens),
                elapsedMs, success, errorCode);
    }

    /** 캐시 히트로 AI를 호출하지 않은 경우 1줄. 토큰 0, 템플릿 미사용. */
    public void logCacheHit(String tripId) {
        usageLog.info("trip_id={} | template=- v0 | cache_hit=true | in_tokens=0 out_tokens=0 | elapsed_ms=0 | success=true",
                nullSafe(tripId));

        // §5.6-a: 캐시 히트도 호출 1건으로 카운트하되 토큰/비용은 0(히트율 계산용).
        persist(tripId, null, null, true, 0, 0, BigDecimal.ZERO, 0L, true, null);
    }

    /**
     * ai_call_log 적재. 기록 실패가 일정 생성/재조정을 깨뜨리면 안 되므로 예외는 삼키고 경고만 남긴다
     * (원본 기록은 이미 ai-usage.log에 있다).
     */
    private void persist(
            String tripId,
            String templateName,
            Integer templateVersion,
            boolean cacheHit,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal cost,
            long elapsedMs,
            boolean success,
            String errorCode) {
        try {
            AiCallLog entity = new AiCallLog();
            entity.setTripId(parseUuid(tripId));
            entity.setTemplateName(templateName);
            entity.setTemplateVersion(templateVersion);
            entity.setCacheHit(cacheHit);
            entity.setInputTokens(inputTokens == null ? 0 : inputTokens);
            entity.setOutputTokens(outputTokens == null ? 0 : outputTokens);
            entity.setCostEstimate(cost);
            entity.setDurationMs((int) Math.min(elapsedMs, Integer.MAX_VALUE));
            entity.setSuccess(success);
            entity.setErrorCode(errorCode);
            entity.setCreatedAt(OffsetDateTime.now());
            aiCallLogRepository.save(entity);
        } catch (RuntimeException e) {
            log.warn("ai_call_log 적재 실패(무시하고 계속 진행)", e);
        }
    }

    /** 최초 생성은 trip이 아직 없어 trip_id가 null/"-"이다. 잘못된 값도 null로 떨어뜨린다. */
    private UUID parseUuid(String tripId) {
        if (tripId == null || tripId.isBlank() || "-".equals(tripId)) {
            return null;
        }
        try {
            return UUID.fromString(tripId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String nullSafe(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
