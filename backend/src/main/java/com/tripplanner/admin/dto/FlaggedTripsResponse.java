package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FlaggedTripsResponse(
        @JsonProperty("flagged_trips") List<FlaggedTripDto> flaggedTrips,
        @JsonProperty("flagged_count") long flaggedCount,
        @JsonProperty("total_days") long totalDays,
        @JsonProperty("flagged_ratio") double flaggedRatio) {
}
