package com.tripplanner.ai;

/** AnthropicClient 호출 결과: 응답 텍스트와 사용량(토큰). 토큰은 usage가 없으면 0. */
public record AiCallResult(String text, int inputTokens, int outputTokens) {
}
