package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActivityResponseDto(
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
