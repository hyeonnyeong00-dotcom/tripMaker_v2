package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TripCreateRequest(
        @NotBlank String destination,
        @JsonProperty("start_date") @NotNull LocalDate startDate,
        @JsonProperty("end_date") @NotNull LocalDate endDate,
        @JsonProperty("budget_level") @NotBlank @Size(max = 30) String budgetLevel,
        @NotEmpty List<String> preferences,
        @JsonProperty("include_nearby") boolean includeNearby) {
}
