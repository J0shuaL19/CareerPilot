package com.careerpilot.controller;

import com.careerpilot.dto.DashboardStatsRange;
import com.careerpilot.dto.DashboardStatsResponse;
import com.careerpilot.service.DashboardStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardStatsService dashboardStatsService;

    public DashboardController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats(
            @RequestParam(defaultValue = "LAST_90_DAYS") DashboardStatsRange range
    ) {
        return dashboardStatsService.getStats(range);
    }
}
