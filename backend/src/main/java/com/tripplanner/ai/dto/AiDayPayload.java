package com.tripplanner.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiDayPayload(
        @JsonProperty("day") int day,
        @JsonProperty("theme") String theme,
        @JsonProperty("activities") List<AiActivityPayload> activities,
        @JsonProperty("route_warning") AiRouteWarningPayload routeWarning) {
}
