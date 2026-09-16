package com.tripplanner.admin;

import com.tripplanner.admin.dto.AiUsageDailyPointDto;
import com.tripplanner.admin.dto.AiUsageDailyResponse;
import com.tripplanner.admin.dto.AiUsageSummaryDto;
import com.tripplanner.admin.dto.ErrorLogDto;
import com.tripplanner.admin.dto.ErrorSummaryItemDto;
import com.tripplanner.admin.dto.ErrorSummaryResponse;
import com.tripplanner.ai.AiCallLogRepository;
import com.tripplanner.ai.AiCostCalculator;
import com.tripplanner.common.ErrorLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 대시보드 집계(§5.6-a). 집계는 DB에서 수행하고, 파일 로그는 파싱하지 않는다.
 */
@Service
public class AdminOpsService {

    /** days/limit 허용 범위 — 범위를 벗어난 값은 에러 대신 경계로 맞춘다(대시보드 조회 편의). */
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 200;

    private final AiCallLogRepository aiCallLogRepository;
    private final ErrorLogRepository errorLogRepository;

    public AdminOpsService(AiCallLogRepository aiCallLogRepository, ErrorLogRepository errorLogRepository) {
        this.aiCallLogRepository = aiCallLogRepository;
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional(readOnly = true)
    public AiUsageSummaryDto aiUsageSummary() {
        List<Object[]> rows = aiCallLogRepository.aggregateSummary();
        Object[] row = rows.isEmpty() ? null : rows.get(0);

        long totalCalls = longAt(row, 0);
        long cacheHits = longAt(row, 1);
        long successCalls = longAt(row, 2);
        BigDecimal totalCost = decimalAt(row, 3);
        long avgDurationMs = longAt(row, 4);
        long inputTokens = longAt(row, 5);
        long outputTokens = longAt(row, 6);

        double cacheHitRate = totalCalls == 0 ? 0.0 : (double) cacheHits / totalCalls;
        double budgetUsedRatio = AiCostCalculator.AI_BUDGET_USD.signum() == 0
                ? 0.0
                : totalCost.divide(AiCostCalculator.AI_BUDGET_USD, 6, RoundingMode.HALF_UP).doubleValue();

        return new AiUsageSummaryDto(
                totalCalls,
                cacheHits,
                cacheHitRate,
                successCalls,
                totalCost,
                AiCostCalculator.AI_BUDGET_USD,
                budgetUsedRatio,
                avgDurationMs,
                inputTokens,
                outputTokens);
    }

    @Transactional(readOnly = true)
    public AiUsageDailyResponse aiUsageDaily(int days) {
        int window = clamp(days, MIN_DAYS, MAX_DAYS);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(window - 1L);
        OffsetDateTime since = from.atStartOfDay().atOffset(ZoneOffset.UTC);

        Map<LocalDate, AiUsageDailyPointDto> byDate = new LinkedHashMap<>();
        for (Object[] row : aiCallLogRepository.aggregateDaily(since)) {
            LocalDate date = toLocalDate(row[0]);
            byDate.put(date, new AiUsageDailyPointDto(
                    date,
                    longAt(row, 1),
                    longAt(row, 2),
                    longAt(row, 3),
                    longAt(row, 4),
                    decimalAt(row, 5)));
        }

        // 호출이 없는 날도 0으로 채워 차트가 끊기지 않게 한다.
        List<AiUsageDailyPointDto> points = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            points.add(byDate.getOrDefault(date,
                    new AiUsageDailyPointDto(date, 0L, 0L, 0L, 0L, BigDecimal.ZERO)));
        }
        return new AiUsageDailyResponse(window, points);
    }

    @Transactional(readOnly = true)
    public ErrorSummaryResponse errorSummary() {
        List<ErrorSummaryItemDto> items = errorLogRepository.aggregateByCode().stream()
                .map(row -> new ErrorSummaryItemDto((String) row[0], (String) row[1], longAt(row, 2)))
                .toList();
        long total = items.stream().mapToLong(ErrorSummaryItemDto::count).sum();
        return new ErrorSummaryResponse(items, total);
    }

    @Transactional(readOnly = true)
    public List<ErrorLogDto> recentErrors(int limit) {
        int size = clamp(limit, MIN_LIMIT, MAX_LIMIT);
        return errorLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size)).stream()
                .map(entity -> new ErrorLogDto(
                        entity.getId(),
                        entity.getErrorCode(),
                        entity.getErrorCategory(),
                        entity.getMessage(),
                        entity.getPath(),
                        entity.getCreatedAt()))
                .toList();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long longAt(Object[] row, int index) {
        if (row == null || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }

    private static BigDecimal decimalAt(Object[] row, int index) {
        if (row == null || row[index] == null) {
            return BigDecimal.ZERO;
        }
        Object value = row[index];
        return value instanceof BigDecimal decimal ? decimal : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    /** native 쿼리의 date 컬럼은 드라이버에 따라 java.sql.Date 또는 LocalDate로 온다. */
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
