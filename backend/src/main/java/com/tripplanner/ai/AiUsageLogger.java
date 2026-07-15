package com.tripplanner.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 사용량 전용 로거. logback의 {@code ai-usage} named logger로 라우팅되어 {@code logs/ai-usage.log}에
 * 한 줄씩 적재된다(시각은 appender 패턴이 붙임). <b>보안: 프롬프트 본문/응답 본문/키는 절대 남기지 않는다.</b>
 * 템플릿은 이름+버전만, 그 외엔 토큰 수·소요 시간·성공 여부만 기록한다.
 */
@Component
public class AiUsageLogger {

    private static final Logger usageLog = LoggerFactory.getLogger("ai-usage");

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
    }

    /** 캐시 히트로 AI를 호출하지 않은 경우 1줄. 토큰 0, 템플릿 미사용. */
    public void logCacheHit(String tripId) {
        usageLog.info("trip_id={} | template=- v0 | cache_hit=true | in_tokens=0 out_tokens=0 | elapsed_ms=0 | success=true",
                nullSafe(tripId));
    }

    private String nullSafe(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
