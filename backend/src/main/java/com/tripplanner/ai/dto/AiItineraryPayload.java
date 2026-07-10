package com.tripplanner.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * ai_response_cache.response_json에 저장되는 형태와 동일하다(CLAUDE.md 5.6-3 — trip_id/meta는
 * 저장 시점에만 정해지므로 캐시 대상에서 제외한다).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiItineraryPayload(
        @JsonProperty("destination") String destination,
        @JsonProperty("duration_days") int durationDays,
        @JsonProperty("summary") String summary,
        @JsonProperty("days") List<AiDayPayload> days) {
}
