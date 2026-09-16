package com.tripplanner.ai;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiCallLogRepository extends JpaRepository<AiCallLog, UUID> {

    /**
     * 요약 집계 1행: [총 호출 수, 캐시 히트 수, 성공 수, 누적 비용, 평균 소요 ms, 입력 토큰 합, 출력 토큰 합].
     * 평균 응답 시간은 실제 AI를 호출한 건만 의미가 있어 캐시 히트(duration 0)는 제외한다.
     */
    @Query("""
            select count(c),
                   coalesce(sum(case when c.cacheHit = true then 1L else 0L end), 0L),
                   coalesce(sum(case when c.success = true then 1L else 0L end), 0L),
                   coalesce(sum(c.costEstimate), 0),
                   coalesce(avg(case when c.cacheHit = false then c.durationMs else null end), 0),
                   coalesce(sum(c.inputTokens), 0L),
                   coalesce(sum(c.outputTokens), 0L)
            from AiCallLog c
            """)
    List<Object[]> aggregateSummary();

    /** 일별 추이: [날짜, 호출 수, 캐시 히트 수, 입력 토큰, 출력 토큰, 비용]. */
    @Query(value = """
            select (created_at at time zone 'UTC')::date as day,
                   count(*)                              as calls,
                   coalesce(sum(case when cache_hit then 1 else 0 end), 0) as cache_hits,
                   coalesce(sum(input_tokens), 0)        as input_tokens,
                   coalesce(sum(output_tokens), 0)       as output_tokens,
                   coalesce(sum(cost_estimate), 0)       as cost
            from ai_call_log
            where created_at >= :since
            group by day
            order by day
            """, nativeQuery = true)
    List<Object[]> aggregateDaily(@Param("since") OffsetDateTime since);
}
