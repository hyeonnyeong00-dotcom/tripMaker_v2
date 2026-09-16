package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** §4 GET /api/admin/errors/summary 응답. */
public record ErrorSummaryResponse(
        @JsonProperty("items") List<ErrorSummaryItemDto> items,
        @JsonProperty("total_errors") long totalErrors) {
}
