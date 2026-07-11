package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DestinationStatsResponse(
        @JsonProperty("destinations") List<DestinationStatDto> destinations,
        @JsonProperty("total_trips") long totalTrips,
        @JsonProperty("period_days") int periodDays) {
}
