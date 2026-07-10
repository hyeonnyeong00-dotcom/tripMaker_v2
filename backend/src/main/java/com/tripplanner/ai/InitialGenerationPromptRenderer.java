package com.tripplanner.ai;

import java.util.List;
import org.springframework.stereotype.Component;

/** prompt_templates.name='initial_generation' 본문의 {{placeholder}}를 요청값으로 치환한다. */
@Component
public class InitialGenerationPromptRenderer {

    public String render(
            PromptTemplate template,
            String destination,
            int durationDays,
            String budgetLevel,
            List<String> preferences,
            boolean includeNearby) {
        String nearbyInstruction = includeNearby
                ? "사용자가 근교 지역 포함을 원한다. 목적지뿐 아니라 인접한 근교 지역의 명소/활동도 추천 범위에 포함해라."
                : "목적지 지역 내에서만 추천하고 근교 지역은 포함하지 마라.";

        return template.getContent()
                .replace("{{destination}}", destination)
                .replace("{{duration_days}}", String.valueOf(durationDays))
                .replace("{{budget_level}}", budgetLevel)
                .replace("{{preferences}}", String.join(", ", preferences))
                .replace("{{nearby_instruction}}", nearbyInstruction);
    }
}
