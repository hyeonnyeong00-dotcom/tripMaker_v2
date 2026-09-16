package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 에러 코드별 발생 횟수 1건. */
public record ErrorSummaryItemDto(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_category") String errorCategory,
        @JsonProperty("count") long count) {
}
