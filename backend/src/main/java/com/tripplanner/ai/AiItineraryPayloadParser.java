package com.tripplanner.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiItineraryPayload;
import org.springframework.stereotype.Component;

/** AI 원시 텍스트 응답을 {@link AiItineraryPayload}로 파싱하고 기본 스키마를 검증한다(초기 생성/재조정 공용). */
@Component
public class AiItineraryPayloadParser {

    private final ObjectMapper objectMapper;

    public AiItineraryPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiItineraryPayload parse(String raw, int expectedDurationDays) {
        String cleaned = stripMarkdownFence(raw);
        AiItineraryPayload payload;
        try {
            payload = objectMapper.readValue(cleaned, AiItineraryPayload.class);
        } catch (Exception e) {
            throw new AiParseException("AI 응답 JSON 파싱 실패. head=" + snippet(cleaned, true)
                    + " tail=" + snippet(cleaned, false), e);
        }

        if (payload.days() == null || payload.days().size() != expectedDurationDays) {
            throw new AiParseException(
                    "AI 응답 day 개수가 duration_days와 불일치: expected=" + expectedDurationDays
                            + " actual=" + (payload.days() == null ? 0 : payload.days().size()));
        }

        for (AiDayPayload day : payload.days()) {
            if (day.activities() == null || day.activities().isEmpty()) {
                throw new AiParseException("day " + day.day() + "에 activities가 없음");
            }
            boolean hasMissingCoordinates = day.activities().stream()
                    .anyMatch(a -> a.lat() == null || a.lng() == null || a.title() == null || a.title().isBlank());
            if (hasMissingCoordinates) {
                throw new AiParseException("day " + day.day() + "의 activity에 필수값(title/lat/lng) 누락");
            }
        }

        return payload;
    }

    /** 파싱 실패 진단용 — 응답 머리/꼬리 120자만 로그에 남긴다(전체 덤프 금지). */
    private String snippet(String text, boolean head) {
        int n = 120;
        String s = text.length() <= n ? text : (head ? text.substring(0, n) : text.substring(text.length() - n));
        return "[" + s.replace("\n", "\\n") + "]";
    }

    private String stripMarkdownFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
