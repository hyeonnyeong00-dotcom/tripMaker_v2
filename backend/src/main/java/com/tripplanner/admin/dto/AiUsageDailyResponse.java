package com.tripplanner.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** §4 GET /api/admin/ai-usage/daily?days=N 응답. 데이터가 없는 날도 0으로 채워 보낸다(차트 연속성). */
public record AiUsageDailyResponse(
        @JsonProperty("days") int days,
        @JsonProperty("points") List<AiUsageDailyPointDto> points) {
}
