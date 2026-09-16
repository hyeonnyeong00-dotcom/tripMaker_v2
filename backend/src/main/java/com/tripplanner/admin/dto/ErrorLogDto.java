package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 최근 에러 목록 1행(시각·코드·메시지·경로). */
public record ErrorLogDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_category") String errorCategory,
        @JsonProperty("message") String message,
        @JsonProperty("path") String path,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
}
