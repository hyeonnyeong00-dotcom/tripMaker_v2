package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record TripResponse(
        @JsonProperty("trip_id") UUID tripId,
        @JsonProperty("destination") String destination,
        @JsonProperty("duration_days") int durationDays,
        @JsonProperty("summary") String summary,
        @JsonProperty("days") List<DayResponseDto> days,
        @JsonProperty("meta") MetaDto meta) {
}
