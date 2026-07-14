package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserDto(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("email") String email,
        @JsonProperty("role") String role,
        @JsonProperty("last_login_at") OffsetDateTime lastLoginAt,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
}
