package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderRequest(
        @NotNull @Min(1) Integer day,
        @JsonProperty("new_activity_order") @NotEmpty List<String> newActivityOrder) {
}
