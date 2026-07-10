package com.tripplanner.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record MeResponse(
        @JsonProperty("user_id") UUID userId,
        String email,
        String role) {
}
