package com.tripplanner.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

public record TripCreateRequest(
        @NotBlank String destination,
        @JsonProperty("start_date") @NotNull LocalDate startDate,
        @JsonProperty("end_date") @NotNull LocalDate endDate,
        @JsonProperty("budget_min") @NotNull @Min(0) Integer budgetMin,
        @JsonProperty("budget_max") @Min(0) Integer budgetMax,
        @NotBlank @Pattern(regexp = "parent|friend|solo|couple|kid|etc") String companion,
        @NotEmpty List<String> preferences,
        @JsonProperty("include_nearby") boolean includeNearby,
        @JsonProperty("active_start_time") @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d") String activeStartTime,
        @JsonProperty("active_end_time") @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d") String activeEndTime) {
}
