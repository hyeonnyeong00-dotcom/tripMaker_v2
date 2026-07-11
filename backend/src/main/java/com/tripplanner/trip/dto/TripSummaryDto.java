package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripSummaryDto(
        @JsonProperty("trip_id") UUID tripId,
        String destination,
        String summary,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("duration_days") int durationDays,
        List<String> preferences) {
}
