package com.careerpilot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobActivityCalendarImportEventResponse;
import com.careerpilot.dto.JobActivityCalendarImportPreviewResponse;
import com.careerpilot.dto.JobActivityCalendarImportResultResponse;
import com.careerpilot.dto.ScheduledJobActivityResponse;
import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.service.JobActivityCalendarFile;
import com.careerpilot.service.JobActivityCalendarImportService;
import com.careerpilot.service.JobActivityCalendarService;
import com.careerpilot.service.JobActivityService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobActivityScheduleController.class)
class JobActivityScheduleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobActivityService jobActivityService;

    @MockitoBean
    private JobActivityCalendarService jobActivityCalendarService;

    @MockitoBean
    private JobActivityCalendarImportService jobActivityCalendarImportService;

    @Test
    void returnsOverdueActivities() throws Exception {
        when(jobActivityService.getOverdueActivities()).thenReturn(List.of(
                new UpcomingJobActivityResponse(
                        2L,
                        1L,
                        "OpenAI",
                        "Engineer",
                        JobActivityType.INTERVIEW,
                        "Missed interview",
                        "Alex Chen",
                        Instant.parse("2026-08-22T12:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/job-activities/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(1))
                .andExpect(jsonPath("$[0].title").value("Missed interview"));
    }

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

    @Test
    void downloadsFilteredCalendarActivities() throws Exception {
        when(jobActivityCalendarService.export(List.of(2L, 3L))).thenReturn(
                new JobActivityCalendarFile(
                        "careerpilot-calendar-20260823T190000Z.ics",
                        "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"
                                .getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(post("/api/job-activities/calendar/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[2,3]}"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo("text/calendar;charset=UTF-8"))
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader("Content-Disposition")
                ).isEqualTo(
                        "attachment; filename=\"careerpilot-calendar-20260823T190000Z.ics\""
                ))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("BEGIN:VCALENDAR"));
    }

    @Test
    void validatesCalendarExportSelection() throws Exception {
        mockMvc.perform(post("/api/job-activities/calendar/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.activityIds")
                        .value("At least one activity is required"));
    }

    @Test
    void previewsCalendarImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "external.ics",
                "text/calendar",
                "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n".getBytes(StandardCharsets.UTF_8)
        );
        when(jobActivityCalendarImportService.preview(any())).thenReturn(
                new JobActivityCalendarImportPreviewResponse(
                        "external.ics",
                        1,
                        1,
                        0,
                        List.of(new JobActivityCalendarImportEventResponse(
                                1,
                                "Technical interview",
                                "Panel",
                                "Zoom",
                                Instant.parse("2026-08-25T16:00:00Z"),
                                JobActivityType.INTERVIEW,
                                true,
                                List.of()
                        ))
                )
        );

        mockMvc.perform(multipart("/api/job-activities/calendar/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("external.ics"))
                .andExpect(jsonPath("$.events[0].title").value("Technical interview"))
                .andExpect(jsonPath("$.events[0].suggestedType").value("INTERVIEW"));
    }

    @Test
    void importsSelectedCalendarEvents() throws Exception {
        when(jobActivityCalendarImportService.importEvents(anyList())).thenReturn(
                new JobActivityCalendarImportResultResponse(1, 1)
        );

        mockMvc.perform(post("/api/job-activities/calendar/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "events": [{
                                    "jobId": 4,
                                    "type": "INTERVIEW",
                                    "title": "Technical interview",
                                    "occurredAt": "2026-08-25T16:00:00Z"
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skippedDuplicates").value(1));
    }

    @Test
    void validatesCalendarImportSelection() throws Exception {
        mockMvc.perform(post("/api/job-activities/calendar/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.events")
                        .value("At least one calendar event is required"));
    }

    @Test
    void returnsCalendarActivitiesForRequestedRange() throws Exception {
        Instant start = Instant.parse("2026-08-01T07:00:00Z");
        Instant end = Instant.parse("2026-09-01T07:00:00Z");
        when(jobActivityService.getCalendarActivities(start, end)).thenReturn(List.of(
                new ScheduledJobActivityResponse(
                        2L,
                        1L,
                        "OpenAI",
                        "Engineer",
                        JobActivityType.INTERVIEW,
                        "Technical interview",
                        "Alex Chen",
                        Instant.parse("2026-08-23T18:00:00Z"),
                        Instant.parse("2026-08-23T19:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/job-activities/calendar")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobTitle").value("Engineer"))
                .andExpect(jsonPath("$[0].completedAt").value("2026-08-23T19:00:00Z"));
    }
}