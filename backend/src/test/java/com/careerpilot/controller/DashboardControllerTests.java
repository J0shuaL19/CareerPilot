package com.careerpilot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.DashboardStatsRange;
import com.careerpilot.dto.DashboardStatsResponse;
import com.careerpilot.service.DashboardStatsService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
class DashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardStatsService dashboardStatsService;

    @Test
    void returnsNinetyDayStatsByDefault() throws Exception {
        when(dashboardStatsService.getStats(DashboardStatsRange.LAST_90_DAYS))
                .thenReturn(stats(DashboardStatsRange.LAST_90_DAYS));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("LAST_90_DAYS"))
                .andExpect(jsonPath("$.trackedJobs").value(10))
                .andExpect(jsonPath("$.applications").value(8))
                .andExpect(jsonPath("$.interviews").value(4))
                .andExpect(jsonPath("$.offers").value(1))
                .andExpect(jsonPath("$.applicationRate").value(80))
                .andExpect(jsonPath("$.interviewRate").value(50))
                .andExpect(jsonPath("$.offerRate").value(25));
    }

    @Test
    void acceptsAnExplicitRange() throws Exception {
        when(dashboardStatsService.getStats(DashboardStatsRange.ALL_TIME))
                .thenReturn(stats(DashboardStatsRange.ALL_TIME));

        mockMvc.perform(get("/api/dashboard/stats").queryParam("range", "ALL_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("ALL_TIME"));
    }

    @Test
    void rejectsAnUnknownRange() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats").queryParam("range", "LAST_YEAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for range"));
    }

    private static DashboardStatsResponse stats(DashboardStatsRange range) {
        return new DashboardStatsResponse(
                range,
                Instant.parse("2026-05-25T12:00:00Z"),
                Instant.parse("2026-08-23T12:00:00Z"),
                10,
                8,
                4,
                1,
                80,
                50,
                25
        );
    }
}
