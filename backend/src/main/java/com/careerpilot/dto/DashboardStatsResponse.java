package com.careerpilot.dto;

import java.time.Instant;

public record DashboardStatsResponse(
        DashboardStatsRange range,
        Instant from,
        Instant to,
        int trackedJobs,
        int applications,
        int interviews,
        int offers,
        Integer applicationRate,
        Integer interviewRate,
        Integer offerRate
) {
}
