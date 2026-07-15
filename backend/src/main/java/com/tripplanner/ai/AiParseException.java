package com.tripplanner.ai;

import com.tripplanner.common.ErrorCodes;

/** AI 응답 텍스트를 JSON으로 파싱하거나 스키마를 검증하는 데 실패했을 때 던진다. */
public class AiParseException extends RuntimeException {

    public AiParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiParseException(String message) {
        super(message);
    }

    /** 세분화 에러 코드(로깅/사용량 기록용). */
    public String code() {
        return ErrorCodes.AI_PARSE_FAILED;
    }
}
