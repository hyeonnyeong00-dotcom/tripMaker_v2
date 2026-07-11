package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DestinationStatDto(
        @JsonProperty("destination") String destination,
        @JsonProperty("count") long count) {
}
