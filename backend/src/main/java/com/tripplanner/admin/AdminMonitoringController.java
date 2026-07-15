package com.tripplanner.admin;

import com.tripplanner.admin.dto.DestinationStatsResponse;
import com.tripplanner.admin.dto.FlaggedTripsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Monitoring", description = "이상 동선 flagged 여행 조회 / 인기 목적지 통계 (admin 전용)")
@RestController
@RequestMapping("/api/admin")
public class AdminMonitoringController {

    private final AdminMonitoringService adminMonitoringService;

    public AdminMonitoringController(AdminMonitoringService adminMonitoringService) {
        this.adminMonitoringService = adminMonitoringService;
    }

    @GetMapping("/flagged-trips")
    public FlaggedTripsResponse flaggedTrips() {
        return adminMonitoringService.flaggedTrips();
    }

    @GetMapping("/stats/destinations")
    public DestinationStatsResponse destinationStats() {
        return adminMonitoringService.destinationStats();
    }
}
