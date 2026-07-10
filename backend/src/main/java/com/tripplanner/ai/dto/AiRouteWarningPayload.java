package com.tripplanner.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRouteWarningPayload(
        @JsonProperty("flagged") boolean flagged,
        @JsonProperty("reason") String reason) {
}
