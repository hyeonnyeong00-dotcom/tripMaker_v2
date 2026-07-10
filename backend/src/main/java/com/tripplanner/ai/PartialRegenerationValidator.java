package com.tripplanner.ai;

import com.tripplanner.ai.dto.AiDayPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * CLAUDE.md 5.1: 재조정 시 요청하지 않은 day가 원본과 다르면(깊은 비교) 위반이다.
 * {@link AiDayPayload}는 record이므로 activities/route_warning까지 자동으로 깊은 equals가 적용된다.
 */
@Component
public class PartialRegenerationValidator {

    /** 반환된 리스트가 비어있지 않으면 위반이며, 각 원소는 원본과 달라진 day 번호다. */
    public List<Integer> findViolatingDays(List<AiDayPayload> original, List<AiDayPayload> candidate, int changedDay) {
        Map<Integer, AiDayPayload> candidateByDay =
                candidate.stream().collect(Collectors.toMap(AiDayPayload::day, Function.identity(), (a, b) -> a));

        List<Integer> violations = new ArrayList<>();
        for (AiDayPayload originalDay : original) {
            if (originalDay.day() == changedDay) {
                continue;
            }
            AiDayPayload candidateDay = candidateByDay.get(originalDay.day());
            if (candidateDay == null || !Objects.equals(originalDay, candidateDay)) {
                violations.add(originalDay.day());
            }
        }
        return violations;
    }
}
