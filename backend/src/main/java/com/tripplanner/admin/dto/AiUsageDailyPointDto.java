package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 일별 추이 1점. */
public record AiUsageDailyPointDto(
        @JsonProperty("date") LocalDate date,
        @JsonProperty("calls") long calls,
        @JsonProperty("cache_hits") long cacheHits,
        @JsonProperty("input_tokens") long inputTokens,
        @JsonProperty("output_tokens") long outputTokens,
        @JsonProperty("cost_usd") BigDecimal costUsd) {
}
