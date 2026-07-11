package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromptTemplateDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("content") String content,
        @JsonProperty("version") int version,
        @JsonProperty("is_active") boolean isActive,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
}
