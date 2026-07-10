package com.tripplanner.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiActivityPayload(
        @JsonProperty("id") String id,
        @JsonProperty("time") String time,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("category") String category,
        @JsonProperty("duration_minutes") Integer durationMinutes,
        @JsonProperty("location") String location,
        @JsonProperty("estimated_cost") Integer estimatedCost,
        @JsonProperty("tips") String tips,
        @JsonProperty("lat") Double lat,
        @JsonProperty("lng") Double lng) {
}
