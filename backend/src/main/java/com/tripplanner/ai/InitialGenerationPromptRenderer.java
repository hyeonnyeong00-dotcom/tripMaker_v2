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

    /**
     * 구간 렌더링: [startDay, endDay]가 전체 여행(totalDays)의 일부면 구간 지시문을,
     * 전체(1~totalDays)면 단일 생성 지시문을 {{segment_instruction}}에 채운다.
     */
    public String render(
            PromptTemplate template,
            String destination,
            int startDay,
            int endDay,
            int totalDays,
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
        int segmentDays = endDay - startDay + 1;

        return template.getContent()
                .replace("{{destination}}", destination)
                .replace("{{duration_days}}", String.valueOf(segmentDays))
                .replace("{{segment_instruction}}", buildSegmentInstruction(startDay, endDay, totalDays))
                .replace("{{budget_range}}", buildBudgetRange(budgetMin, budgetMax))
                .replace("{{companion_instruction}}", buildCompanionInstruction(companion))
                .replace("{{preferences}}", String.join(", ", preferences))
                .replace("{{nearby_instruction}}", nearbyInstruction)
                .replace("{{active_time_range}}", activeStartTime + "~" + activeEndTime);
    }

    private String buildSegmentInstruction(int startDay, int endDay, int totalDays) {
        if (startDay == 1 && endDay == totalDays) {
            return "여행 전체(" + totalDays + "일)를 한 번에 생성한다. day 번호는 1부터 " + totalDays + "까지 순서대로 사용해라.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("전체 ").append(totalDays).append("일 여행 중 ")
                .append(startDay).append("~").append(endDay).append("일차 구간만 생성한다. ")
                .append("days 배열에는 이 구간의 ").append(endDay - startDay + 1).append("일만 담고, ")
                .append("day 번호는 ").append(startDay).append("부터 ").append(endDay).append("까지 순서대로 사용해라. ");
        if (startDay == 1) {
            sb.append("여행 첫날(도착)이 포함된 구간이다. ");
        } else if (endDay == totalDays) {
            sb.append("여행 마지막 날(마무리)이 포함된 구간이다. 도착 일정은 넣지 마라. ");
        } else {
            sb.append("여행 중반 구간이므로 도착/마무리 성격의 일정은 넣지 마라. ");
        }
        sb.append("같은 여행의 다른 구간과 장소가 겹치지 않도록 이 구간에서는 ")
                .append(startDay == 1 ? "가장 대표적인 명소" : "덜 알려진 지역·테마까지 폭넓게")
                .append(" 다뤄라. summary에는 구간이 아니라 여행 전체 컨셉을 한 줄로 쓰되, 일수나 구간 정보는 언급하지 마라.");
        return sb.toString();
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
