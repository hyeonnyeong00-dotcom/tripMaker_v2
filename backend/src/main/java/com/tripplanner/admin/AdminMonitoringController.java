package com.tripplanner.admin;

import com.tripplanner.admin.dto.DestinationStatsResponse;
import com.tripplanner.admin.dto.FlaggedTripsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
