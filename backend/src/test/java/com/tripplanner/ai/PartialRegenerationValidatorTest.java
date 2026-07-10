package com.tripplanner.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripplanner.ai.dto.AiActivityPayload;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiRouteWarningPayload;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CLAUDE.md 5.1: 프로젝트 유일 필수 단위 테스트 — 요청하지 않은 day가 원본과 다르면 위반. */
class PartialRegenerationValidatorTest {

    private final PartialRegenerationValidator validator = new PartialRegenerationValidator();

    private static AiActivityPayload activity(String id, String title) {
        return new AiActivityPayload(id, "09:00", title, "설명", "관광지", 60, "위치", 1000, null, 37.5, 127.0);
    }

    private static AiDayPayload day(int dayNumber, String theme, AiActivityPayload... activities) {
        return new AiDayPayload(dayNumber, theme, List.of(activities), new AiRouteWarningPayload(false, null));
    }

    @Test
    void 요청하지_않은_day가_원본과_동일하면_통과한다() {
        List<AiDayPayload> original = List.of(
                day(1, "1일차", activity("d1-a1", "활동1")),
                day(2, "2일차", activity("d2-a1", "활동2-1"), activity("d2-a2", "활동2-2")),
                day(3, "3일차", activity("d3-a1", "활동3")));

        // day 2만 순서가 바뀐 새 응답(다른 날짜는 원본과 완전히 동일)
        List<AiDayPayload> candidate = List.of(
                day(1, "1일차", activity("d1-a1", "활동1")),
                day(2, "2일차", activity("d2-a2", "활동2-2"), activity("d2-a1", "활동2-1")),
                day(3, "3일차", activity("d3-a1", "활동3")));

        List<Integer> violations = validator.findViolatingDays(original, candidate, 2);

        assertThat(violations).isEmpty();
    }

    @Test
    void 요청하지_않은_day가_원본과_다르면_위반이다() {
        List<AiDayPayload> original = List.of(
                day(1, "1일차", activity("d1-a1", "활동1")),
                day(2, "2일차", activity("d2-a1", "활동2-1")),
                day(3, "3일차", activity("d3-a1", "활동3")));

        // day 2를 요청했는데 day 3(요청 안 한 날짜)의 테마가 원본과 달라짐 → 위반
        List<AiDayPayload> candidate = List.of(
                day(1, "1일차", activity("d1-a1", "활동1")),
                day(2, "2일차", activity("d2-a1", "활동2-1")),
                day(3, "완전히 다른 테마", activity("d3-a1", "활동3")));

        List<Integer> violations = validator.findViolatingDays(original, candidate, 2);

        assertThat(violations).containsExactly(3);
    }

    @Test
    void 위반_후_재시도한_응답이_원본과_일치하면_통과한다() {
        List<AiDayPayload> original = List.of(
                day(1, "1일차", activity("d1-a1", "활동1")),
                day(2, "2일차", activity("d2-a1", "활동2-1")),
                day(3, "3일차", activity("d3-a1", "활동3")));

        // 1차 시도: day 1(요청 안 한 날짜)이 누락됨 → 위반
        List<AiDayPayload> firstAttempt = List.of(
                day(2, "2일차", activity("d2-a1", "활동2-1")),
                day(3, "3일차", activity("d3-a1", "활동3")));
        List<Integer> firstViolations = validator.findViolatingDays(original, firstAttempt, 2);
        assertThat(firstViolations).containsExactly(1);

        // 재시도 응답: 다른 날짜가 모두 원본과 일치 → 통과
        List<AiDayPayload> retryAttempt = List.of(
                day(1, "1일차", activity("d1-a1", "활동1")),
                day(2, "2일차", activity("d2-a1", "활동2-1")),
                day(3, "3일차", activity("d3-a1", "활동3")));
        List<Integer> retryViolations = validator.findViolatingDays(original, retryAttempt, 2);
        assertThat(retryViolations).isEmpty();
    }
}
