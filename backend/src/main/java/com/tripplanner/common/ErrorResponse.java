package com.tripplanner.common;

/**
 * 공통 에러 응답. {@code error}(기존 계약 — 프론트 분기용, 변경 금지)와 HTTP 상태는 그대로 두고,
 * 세분화된 {@code code}("ERR_xxx", {@link ErrorCodes})를 추가로 노출한다.
 */
public record ErrorResponse(String error, String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode, String code, String message) {
        return new ErrorResponse(errorCode.name(), code, message);
    }
}
