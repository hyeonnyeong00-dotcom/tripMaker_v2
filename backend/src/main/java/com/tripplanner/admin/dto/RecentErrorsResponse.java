package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** §4 GET /api/admin/errors/recent?limit=N 응답. */
public record RecentErrorsResponse(
        @JsonProperty("limit") int limit,
        @JsonProperty("errors") List<ErrorLogDto> errors) {
}
