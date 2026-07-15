package com.tripplanner.ai;

import com.tripplanner.common.ErrorCodes;

/** 재조정 응답이 요청하지 않은 day를 변경했거나(부분 재생성 위반), 사용자가 정한 순서를 임의로 바꿨을 때 던진다. */
public class PartialRegenerationViolationException extends RuntimeException {

    public PartialRegenerationViolationException(String message) {
        super(message);
    }

    /** 세분화 에러 코드(로깅/사용량 기록용). */
    public String code() {
        return ErrorCodes.PARTIAL_REGEN_VIOLATION;
    }
}
