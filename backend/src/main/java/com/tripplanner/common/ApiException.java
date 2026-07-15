package com.tripplanner.common;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String code;

    public ApiException(ErrorCode errorCode, String code, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = code;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    /** 세분화된 에러 코드("ERR_xxx", {@link ErrorCodes}). */
    public String code() {
        return code;
    }
}
