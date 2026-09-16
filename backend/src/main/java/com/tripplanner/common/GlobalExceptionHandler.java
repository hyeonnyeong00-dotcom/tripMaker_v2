package com.tripplanner.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorLogWriter errorLogWriter;

    public GlobalExceptionHandler(ErrorLogWriter errorLogWriter) {
        this.errorLogWriter = errorLogWriter;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        // 코드 추적을 위해 업무 에러도 error.log(WARN+)에 [코드]와 함께 남긴다(중복 이메일/로그인 실패 등 4xx 포함).
        // 5xx는 서버측 심각도라 ERROR로 격상. 스택트레이스는 남기지 않아 소음을 줄인다.
        String line = "[{}] {} — {}";
        if (ex.errorCode().status().is5xxServerError()) {
            log.error(line, ex.code(), ex.errorCode().name(), ex.getMessage());
        } else {
            log.warn(line, ex.code(), ex.errorCode().name(), ex.getMessage());
        }
        return build(ex.errorCode(), ex.code(), ex.getMessage(), path(request));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return buildObject(ErrorCode.VALIDATION_ERROR, ErrorCodes.VALIDATION_GENERIC, message, path(request));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildObject(ErrorCode.VALIDATION_ERROR, ErrorCodes.BODY_UNREADABLE,
                "요청 본문을 읽을 수 없습니다.", path(request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_ERROR, ErrorCodes.PARAM_TYPE_MISMATCH,
                ex.getName() + " 값이 올바르지 않습니다.", path(request));
    }

    /** DB 접근 실패(연결 끊김/제약 위반 등)는 STORAGE_ERROR(503)로 공통 계약에 실어 보낸다. 상세는 로그에만 남긴다. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("[{}] DB 접근 실패", ErrorCodes.DB_ACCESS_FAILED, ex);
        return build(ErrorCode.STORAGE_ERROR, ErrorCodes.DB_ACCESS_FAILED,
                "일시적으로 데이터를 처리할 수 없습니다. 잠시 후 다시 시도해주세요.", path(request));
    }

    /**
     * 위 어느 핸들러에도 걸리지 않은 예상 밖 예외의 최종 안전망. 계약 밖 500(Whitelabel/스택 노출)을 막고
     * INTERNAL_ERROR로 통일한다. 원인/스택은 로그에만 남기고 클라이언트엔 일반 메시지만 노출한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("[{}] 처리되지 않은 예외", ErrorCodes.INTERNAL, ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCodes.INTERNAL,
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", path(request));
    }

    /**
     * <b>에러 응답을 만드는 유일한 지점</b>(§5.6-a). 여기서만 error_log에 적재한다.
     * 적재 실패는 {@link ErrorLogWriter} 안에서 삼켜지므로 응답 계약에는 영향이 없다.
     */
    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode, String code, String message, String path) {
        errorLogWriter.write(errorCode, code, message, path);
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, code, message));
    }

    /** ResponseEntityExceptionHandler 오버라이드용(반환 타입이 {@code ResponseEntity<Object>}). */
    private ResponseEntity<Object> buildObject(ErrorCode errorCode, String code, String message, String path) {
        ResponseEntity<ErrorResponse> response = build(errorCode, code, message, path);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private String path(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }
}
