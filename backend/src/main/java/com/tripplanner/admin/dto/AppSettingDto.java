package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record AppSettingDto(
        @JsonProperty("value") String value,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
}
