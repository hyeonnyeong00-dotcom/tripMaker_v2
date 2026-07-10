package com.tripplanner.ai;

/** 재조정 응답이 요청하지 않은 day를 변경했거나(부분 재생성 위반), 사용자가 정한 순서를 임의로 바꿨을 때 던진다. */
public class PartialRegenerationViolationException extends RuntimeException {

    public PartialRegenerationViolationException(String message) {
        super(message);
    }
}
