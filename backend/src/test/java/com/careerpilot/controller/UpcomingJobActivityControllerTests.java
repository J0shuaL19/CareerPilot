package com.careerpilot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.service.JobActivityService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UpcomingJobActivityController.class)
class UpcomingJobActivityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobActivityService jobActivityService;

    @Test
    void returnsUpcomingActivities() throws Exception {
        when(jobActivityService.getUpcomingActivities()).thenReturn(List.of(
                new UpcomingJobActivityResponse(
                        2L,
                        1L,
                        "OpenAI",
                        "Engineer",
                        JobActivityType.INTERVIEW,
                        "Technical interview",
                        "Alex Chen",
                        Instant.parse("2026-08-23T12:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/job-activities/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(1))
                .andExpect(jsonPath("$[0].company").value("OpenAI"))
                .andExpect(jsonPath("$[0].type").value("INTERVIEW"));
    }
}
