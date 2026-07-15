package com.tripplanner.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    GENERATION_FAILED(HttpStatus.BAD_GATEWAY),
    AUTH_ERROR(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    STORAGE_ERROR(HttpStatus.SERVICE_UNAVAILABLE),
    // 어느 핸들러에도 매핑되지 않은 예상 밖 예외의 공통 계약용(상세는 로그에만, 클라이언트엔 일반 메시지)
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
