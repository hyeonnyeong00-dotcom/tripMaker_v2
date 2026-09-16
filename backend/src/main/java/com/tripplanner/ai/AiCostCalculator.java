package com.tripplanner.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * AI 호출 비용 산출(§5.6-a). <b>단가 상수는 이 파일에만 둔다</b> — 모델을 바꾸면 아래 두 상수만 갱신하면 된다.
 * <p>현재 사용 모델 {@code claude-haiku-4-5}의 공식 단가: 입력 $1.00 / 출력 $5.00 per 1M tokens.
 */
@Component
public class AiCostCalculator {

    /** 입력 토큰 100만 개당 USD 단가. */
    public static final BigDecimal AI_INPUT_PRICE_PER_M = new BigDecimal("1.00");
    /** 출력 토큰 100만 개당 USD 단가. */
    public static final BigDecimal AI_OUTPUT_PRICE_PER_M = new BigDecimal("5.00");
    /** 프로젝트 AI 실비용 상한(§1). 대시보드 게이지가 이 값 대비 진행률을 표시한다. */
    public static final BigDecimal AI_BUDGET_USD = new BigDecimal("10.00");

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    /** ai_call_log.cost_estimate가 numeric(10,4)이므로 같은 소수 자리로 맞춘다. */
    private static final int SCALE = 4;

    /** 캐시 히트·호출 실패처럼 토큰이 없으면 0을 돌려준다. */
    public BigDecimal estimate(Integer inputTokens, Integer outputTokens) {
        BigDecimal input = price(inputTokens, AI_INPUT_PRICE_PER_M);
        BigDecimal output = price(outputTokens, AI_OUTPUT_PRICE_PER_M);
        return input.add(output).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal price(Integer tokens, BigDecimal pricePerMillion) {
        if (tokens == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(tokens)
                .multiply(pricePerMillion)
                .divide(MILLION, SCALE, RoundingMode.HALF_UP);
    }
}
