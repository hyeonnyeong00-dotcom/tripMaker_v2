package com.tripplanner.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 에러 응답 1건의 집계용 기록(§5.6-a). {@link GlobalExceptionHandler}의 응답 생성 지점 한 곳에서만 적재한다.
 * <p><b>보안:</b> 스택트레이스·요청 본문은 담지 않는다. 클라이언트에 노출한 메시지만 저장한다.
 */
@Entity
@Table(name = "error_log")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 세분화 코드("ERR_xxx"). */
    @Column(name = "error_code", nullable = false)
    private String errorCode;

    /** 대분류({@link ErrorCode} 이름) — 응답의 {@code error} 필드와 같은 값. */
    @Column(name = "error_category", nullable = false)
    private String errorCategory;

    @Column
    private String message;

    @Column
    private String path;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
