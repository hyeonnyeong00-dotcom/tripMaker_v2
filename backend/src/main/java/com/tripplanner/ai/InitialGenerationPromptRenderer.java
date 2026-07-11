package com.tripplanner.ai;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** prompt_templates.name='initial_generation' 본문의 {{placeholder}}를 요청값으로 치환한다. */
@Component
public class InitialGenerationPromptRenderer {

    private static final Map<String, String> COMPANION_INSTRUCTIONS = Map.of(
            "parent", "부모님과 함께하는 여행이다. 어르신이 편안하게 다닐 수 있는, 이동이 과하지 않고 여유로운 장소 위주로 추천해라.",
            "friend", "친구들과 함께하는 여행이다. 그룹으로 즐기기 좋은 액티비티·맛집·사진 명소를 우선 추천해라.",
            "solo", "혼자 하는 여행이다. 안전하고 편하게 혼자 즐길 수 있는 장소 위주로 추천해라.",
            "couple", "연인과 함께하는 여행이다. 로맨틱한 분위기의 장소와 액티비티를 우선 추천해라.",
            "kid", "아이와 함께하는 여행이다. 아이 동반이 가능하고 안전한 장소 위주로 추천해라.",
            "etc", "다양한 구성의 동반자와 함께하는 여행이다. 무난하게 즐길 수 있는 장소 위주로 추천해라.");

    public String render(
            PromptTemplate template,
            String destination,
            int durationDays,
            Integer budgetMin,
            Integer budgetMax,
            String companion,
            List<String> preferences,
            boolean includeNearby,
            String activeStartTime,
            String activeEndTime) {
        String nearbyInstruction = includeNearby
                ? "사용자가 근교 지역 포함을 원한다. 목적지뿐 아니라 인접한 근교 지역의 명소/활동도 추천 범위에 포함해라."
                : "목적지 지역 내에서만 추천하고 근교 지역은 포함하지 마라.";

        return template.getContent()
                .replace("{{destination}}", destination)
                .replace("{{duration_days}}", String.valueOf(durationDays))
                .replace("{{budget_range}}", buildBudgetRange(budgetMin, budgetMax))
                .replace("{{companion_instruction}}", buildCompanionInstruction(companion))
                .replace("{{preferences}}", String.join(", ", preferences))
                .replace("{{nearby_instruction}}", nearbyInstruction)
                .replace("{{active_time_range}}", activeStartTime + "~" + activeEndTime);
    }

    private String buildBudgetRange(Integer budgetMin, Integer budgetMax) {
        if (budgetMax == null) {
            return budgetMin + "원 이상, 상한 없음";
        }
        return budgetMin + "원 ~ " + budgetMax + "원";
    }

    private String buildCompanionInstruction(String companion) {
        return COMPANION_INSTRUCTIONS.getOrDefault(companion, COMPANION_INSTRUCTIONS.get("etc"));
    }
}
