package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** §4 GET /api/admin/ai-usage/summary 응답. */
public record AiUsageSummaryDto(
        @JsonProperty("total_calls") long totalCalls,
        @JsonProperty("cache_hits") long cacheHits,
        @JsonProperty("cache_hit_rate") double cacheHitRate,
        @JsonProperty("success_calls") long successCalls,
        @JsonProperty("total_cost_usd") BigDecimal totalCostUsd,
        @JsonProperty("budget_usd") BigDecimal budgetUsd,
        @JsonProperty("budget_used_ratio") double budgetUsedRatio,
        @JsonProperty("avg_duration_ms") long avgDurationMs,
        @JsonProperty("total_input_tokens") long totalInputTokens,
        @JsonProperty("total_output_tokens") long totalOutputTokens) {
}
