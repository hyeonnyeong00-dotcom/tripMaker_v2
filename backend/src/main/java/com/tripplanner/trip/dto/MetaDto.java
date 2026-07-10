package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record MetaDto(
        @JsonProperty("generated_at") OffsetDateTime generatedAt,
        @JsonProperty("revision") int revision) {
}
