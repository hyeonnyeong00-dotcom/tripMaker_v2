package com.tripplanner.ai;

/** AI 응답 텍스트를 JSON으로 파싱하거나 스키마를 검증하는 데 실패했을 때 던진다. */
public class AiParseException extends RuntimeException {

    public AiParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiParseException(String message) {
        super(message);
    }
}
