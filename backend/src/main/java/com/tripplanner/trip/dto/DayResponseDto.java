package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DayResponseDto(
        @JsonProperty("day") int day,
        @JsonProperty("theme") String theme,
        @JsonProperty("activities") List<ActivityResponseDto> activities,
        @JsonProperty("last_modified") boolean lastModified,
        @JsonProperty("route_warning") RouteWarningDto routeWarning) {
}
