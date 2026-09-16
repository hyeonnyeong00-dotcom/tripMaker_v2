package com.tripplanner.admin;

import com.tripplanner.admin.dto.AiUsageDailyResponse;
import com.tripplanner.admin.dto.AiUsageSummaryDto;
import com.tripplanner.admin.dto.ErrorSummaryResponse;
import com.tripplanner.admin.dto.RecentErrorsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Ops", description = "AI 사용량/비용 집계, 에러 모니터링 (admin 전용)")
@RestController
@RequestMapping("/api/admin")
public class AdminOpsController {

    private static final int DEFAULT_DAYS = 14;
    private static final int DEFAULT_LIMIT = 50;

    private final AdminOpsService adminOpsService;

    public AdminOpsController(AdminOpsService adminOpsService) {
        this.adminOpsService = adminOpsService;
    }

    @Operation(summary = "AI 사용량 요약", description = "총 호출 수 / 캐시 히트율 / 누적 예상 비용($10 대비) / 평균 응답시간")
    @GetMapping("/ai-usage/summary")
    public AiUsageSummaryDto aiUsageSummary() {
        return adminOpsService.aiUsageSummary();
    }

    @Operation(summary = "AI 사용량 일별 추이", description = "최근 N일(기본 14일)의 호출 수·토큰·비용. 호출 없는 날은 0으로 채운다.")
    @GetMapping("/ai-usage/daily")
    public AiUsageDailyResponse aiUsageDaily(@RequestParam(name = "days", defaultValue = "" + DEFAULT_DAYS) int days) {
        return adminOpsService.aiUsageDaily(days);
    }

    @Operation(summary = "에러 코드별 발생 횟수", description = "error_log 전체를 코드별로 집계(내림차순)")
    @GetMapping("/errors/summary")
    public ErrorSummaryResponse errorSummary() {
        return adminOpsService.errorSummary();
    }

    @Operation(summary = "최근 에러 목록", description = "최신순 N건(기본 50). 시각·코드·메시지·경로")
    @GetMapping("/errors/recent")
    public RecentErrorsResponse recentErrors(
            @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return new RecentErrorsResponse(limit, adminOpsService.recentErrors(limit));
    }
}
