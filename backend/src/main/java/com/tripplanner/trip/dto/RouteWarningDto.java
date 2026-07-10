package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RouteWarningDto(
        @JsonProperty("flagged") boolean flagged,
        @JsonProperty("reason") String reason) {
}
