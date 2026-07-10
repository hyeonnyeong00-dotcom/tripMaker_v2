package com.tripplanner.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.ai.dto.AiItineraryPayload;
import java.util.List;
import org.springframework.stereotype.Component;

/** prompt_templates.name='reorder' 본문의 {{placeholder}}를 요청값으로 치환한다. */
@Component
public class ReorderPromptRenderer {

    private final ObjectMapper objectMapper;

    public ReorderPromptRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(
            PromptTemplate template, AiItineraryPayload originalItinerary, int day, List<String> newActivityOrder) {
        try {
            String originalJson = objectMapper.writeValueAsString(originalItinerary);
            String newOrderJson = objectMapper.writeValueAsString(newActivityOrder);
            return template.getContent()
                    .replace("{{original_itinerary_json}}", originalJson)
                    .replace("{{day}}", String.valueOf(day))
                    .replace("{{new_activity_order}}", newOrderJson);
        } catch (Exception e) {
            throw new IllegalStateException("재조정 프롬프트 렌더링 실패", e);
        }
    }
}
