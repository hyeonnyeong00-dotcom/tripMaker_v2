package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FlaggedTripDto(
        @JsonProperty("trip_id") UUID tripId,
        @JsonProperty("destination") String destination,
        @JsonProperty("day") int day,
        @JsonProperty("theme") String theme,
        @JsonProperty("reason") String reason,
        @JsonProperty("flagged_at") OffsetDateTime flaggedAt) {
}
