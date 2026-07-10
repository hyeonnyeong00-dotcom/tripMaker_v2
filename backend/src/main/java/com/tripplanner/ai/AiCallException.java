package com.tripplanner.ai;

/** AI API 호출 자체(네트워크/타임아웃/HTTP 오류)가 실패했을 때 던진다. */
public class AiCallException extends RuntimeException {

    public AiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
