package com.careerpilot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.service.JobActivityService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobActivityController.class)
class JobActivityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobActivityService jobActivityService;

    @Test
    void createsActivity() throws Exception {
        when(jobActivityService.createActivity(any(Long.class), any(JobActivityRequest.class)))
                .thenReturn(response(2L, "Technical interview"));

        mockMvc.perform(post("/api/jobs/1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "INTERVIEW",
                                  "title": "Technical interview",
                                  "details": "System design round",
                                  "contact": "Alex Chen",
                                  "occurredAt": "2026-08-25T18:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.type").value("INTERVIEW"));
    }

    @Test
    void listsActivities() throws Exception {
        when(jobActivityService.getActivities(1L)).thenReturn(List.of(
                response(2L, "Interview"),
                response(1L, "Applied")
        ));

        mockMvc.perform(get("/api/jobs/1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Interview"))
                .andExpect(jsonPath("$[1].title").value("Applied"));
    }

    @Test
    void validatesActivityRequest() throws Exception {
        mockMvc.perform(post("/api/jobs/1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": null,
                                  "title": "",
                                  "occurredAt": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").value("Activity type is required"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Activity title is required"))
                .andExpect(jsonPath("$.fieldErrors.occurredAt").value("Activity time is required"));
    }

    @Test
    void rejectsUnknownActivityType() throws Exception {
        mockMvc.perform(post("/api/jobs/1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PHONE_SCREEN",
                                  "title": "Call",
                                  "occurredAt": "2026-08-25T18:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void deletesActivity() throws Exception {
        mockMvc.perform(delete("/api/jobs/1/activities/2"))
                .andExpect(status().isNoContent());
    }

    private static JobActivityResponse response(Long id, String title) {
        return new JobActivityResponse(
                id,
                1L,
                JobActivityType.INTERVIEW,
                title,
                "System design round",
                "Alex Chen",
                Instant.parse("2026-08-25T18:00:00Z"),
                Instant.parse("2026-08-22T12:00:00Z")
        );
    }
}
