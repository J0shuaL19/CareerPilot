package com.careerpilot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.exception.JobActivityCalendarException;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.service.JobActivityCalendarFile;
import com.careerpilot.service.JobActivityCalendarService;
import com.careerpilot.service.JobActivityService;
import java.nio.charset.StandardCharsets;
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

    @MockitoBean
    private JobActivityCalendarService jobActivityCalendarService;

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
    void updatesActivity() throws Exception {
        when(jobActivityService.updateActivity(
                any(Long.class),
                any(Long.class),
                any(JobActivityRequest.class)
        )).thenReturn(response(2L, "Recruiter follow-up"));

        mockMvc.perform(put("/api/jobs/1/activities/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "FOLLOW_UP",
                                  "title": "Recruiter follow-up",
                                  "details": "Send thank-you note",
                                  "contact": "Alex Chen",
                                  "occurredAt": "2026-08-26T18:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Recruiter follow-up"));
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
    void completesActivity() throws Exception {
        when(jobActivityService.completeActivity(any(Long.class), any(Long.class), any()))
                .thenReturn(completedResponse(2L, "Technical interview"));

        mockMvc.perform(put("/api/jobs/1/activities/2/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Strong conversation",
                                  "jobStatus": "INTERVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value("2026-08-24T18:00:00Z"))
                .andExpect(jsonPath("$.completionNote").value("Strong conversation"));
    }

    @Test
    void validatesCompletionNoteLength() throws Exception {
        String note = "a".repeat(2001);

        mockMvc.perform(put("/api/jobs/1/activities/2/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"" + note + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.note").value(
                        "Completion note must be 2000 characters or fewer"
                ));
    }

    @Test
    void deletesActivity() throws Exception {
        mockMvc.perform(delete("/api/jobs/1/activities/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void downloadsActivityCalendar() throws Exception {
        when(jobActivityCalendarService.export(1L, 2L)).thenReturn(
                new JobActivityCalendarFile(
                        "careerpilot-activity-2.ics",
                        "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"
                                .getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(get("/api/jobs/1/activities/2/calendar"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo("text/calendar;charset=UTF-8"))
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader("Content-Disposition")
                ).isEqualTo("attachment; filename=\"careerpilot-activity-2.ics\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("BEGIN:VCALENDAR"));
    }

    @Test
    void rejectsCalendarExportForUnsupportedActivityType() throws Exception {
        when(jobActivityCalendarService.export(1L, 2L)).thenThrow(
                new JobActivityCalendarException(
                        "Only interviews and follow-ups can be exported to a calendar."
                )
        );

        mockMvc.perform(get("/api/jobs/1/activities/2/calendar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only interviews and follow-ups can be exported to a calendar."
                ));
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
                null,
                null,
                Instant.parse("2026-08-22T12:00:00Z")
        );
    }
    private static JobActivityResponse completedResponse(Long id, String title) {
        return new JobActivityResponse(
                id,
                1L,
                JobActivityType.INTERVIEW,
                title,
                "System design round",
                "Alex Chen",
                Instant.parse("2026-08-25T18:00:00Z"),
                Instant.parse("2026-08-24T18:00:00Z"),
                "Strong conversation",
                Instant.parse("2026-08-22T12:00:00Z")
        );
    }
}
