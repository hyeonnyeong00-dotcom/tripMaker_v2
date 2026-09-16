package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 템플릿 버전 스냅샷. 목록 응답에서는 {@code content}가 null이고(번호·수정일만 노출),
 * 특정 버전 조회에서는 본문까지 채워 보낸다.
 */
public record PromptTemplateRevisionDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("template_id") UUID templateId,
        @JsonProperty("version") int version,
        @JsonProperty("content") String content,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
}
