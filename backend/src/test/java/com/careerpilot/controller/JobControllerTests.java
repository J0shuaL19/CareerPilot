package com.careerpilot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobAttentionBulkClearRequest;
import com.careerpilot.dto.JobAttentionBulkRestoreRequest;
import com.careerpilot.dto.JobAttentionBulkSnoozeRequest;
import com.careerpilot.dto.JobAttentionSnoozeRequest;
import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobCsvImportPreviewResponse;
import com.careerpilot.dto.JobCsvImportResultResponse;
import com.careerpilot.dto.JobCsvImportRowResponse;
import com.careerpilot.dto.JobCsvImportRowState;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.dto.JobStatusUpdateRequest;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.JobStatus;
import com.careerpilot.service.JobCsvExportService;
import com.careerpilot.service.JobCsvFile;
import com.careerpilot.service.JobCsvImportService;
import com.careerpilot.service.JobService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobController.class)
class JobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JobCsvExportService jobCsvExportService;

    @MockitoBean
    private JobCsvImportService jobCsvImportService;

    @Test
    void createsJob() throws Exception {
        when(jobService.createJob(any(JobRequest.class))).thenReturn(jobResponse(1L, "OpenAI"));

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company": "OpenAI",
                                  "title": "Software Engineer",
                                  "description": "Build reliable products.",
                                  "jobUrl": "https://example.com/jobs/1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.company").value("OpenAI"))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void returnsJobs() throws Exception {
        when(jobService.getJobs()).thenReturn(List.of(
                jobResponse(2L, "Company B"),
                jobResponse(1L, "Company A")
        ));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void exportsSelectedJobsAsCsvAttachment() throws Exception {
        byte[] content = "\uFEFFID,Company\r\n1,OpenAI\r\n".getBytes(StandardCharsets.UTF_8);
        when(jobCsvExportService.export(List.of(2L, 1L)))
                .thenReturn(new JobCsvFile("careerpilot-jobs.csv", content));

        mockMvc.perform(post("/api/jobs/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobIds": [2, 1]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo("text/csv;charset=UTF-8"))
                .andExpect(result -> assertThat(result.getResponse().getHeader("Content-Disposition"))
                        .isEqualTo("attachment; filename=\"careerpilot-jobs.csv\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .isEqualTo(content));
    }

    @Test
    void rejectsCsvExportWithoutJobs() throws Exception {
        mockMvc.perform(post("/api/jobs/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobIds")
                        .value("At least one job is required"));
    }

    @Test
    void previewsCsvImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                "Company,Title,Description".getBytes(StandardCharsets.UTF_8)
        );
        when(jobCsvImportService.preview(any())).thenReturn(new JobCsvImportPreviewResponse(
                "jobs.csv",
                1,
                1,
                0,
                0,
                List.of(new JobCsvImportRowResponse(
                        2,
                        "OpenAI",
                        "Engineer",
                        "Description",
                        null,
                        JobStatus.SAVED,
                        JobCsvImportRowState.VALID,
                        List.of()
                ))
        ));

        mockMvc.perform(multipart("/api/jobs/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("jobs.csv"))
                .andExpect(jsonPath("$.validRows").value(1))
                .andExpect(jsonPath("$.rows[0].state").value("VALID"));
    }

    @Test
    void importsCsvJobs() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                "Company,Title,Description".getBytes(StandardCharsets.UTF_8)
        );
        when(jobCsvImportService.importFile(any()))
                .thenReturn(new JobCsvImportResultResponse(2, 1, 1));

        mockMvc.perform(multipart("/api/jobs/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skippedDuplicates").value(1))
                .andExpect(jsonPath("$.skippedInvalid").value(1));
    }

    @Test
    void returnsJobById() throws Exception {
        when(jobService.getJob(1L)).thenReturn(jobResponse(1L, "OpenAI"));

        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company": "",
                                  "title": "",
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.company").value("Company is required"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.fieldErrors.description").value("Description is required"));
    }

    @Test
    void returnsNotFoundError() throws Exception {
        when(jobService.getJob(999L)).thenThrow(new ResourceNotFoundException("Job", 999L));

        mockMvc.perform(get("/api/jobs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"))
                .andExpect(jsonPath("$.path").value("/api/jobs/999"));
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void returnsBadRequestForInvalidIdType() throws Exception {
        mockMvc.perform(get("/api/jobs/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for id"));
    }

    @Test
    void updatesJobStatus() throws Exception {
        when(jobService.updateJobStatus(any(Long.class), any(JobStatusUpdateRequest.class)))
                .thenReturn(jobResponse(1L, "OpenAI", JobStatus.INTERVIEW));

        mockMvc.perform(patch("/api/jobs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INTERVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void rejectsMissingJobStatus() throws Exception {
        mockMvc.perform(patch("/api/jobs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("Job status is required"));
    }

    @Test
    void rejectsUnknownJobStatus() throws Exception {
        mockMvc.perform(patch("/api/jobs/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "WAITING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void updatesJobDetails() throws Exception {
        when(jobService.updateJob(any(Long.class), any(JobRequest.class)))
                .thenReturn(jobResponse(1L, "Anthropic"));

        mockMvc.perform(put("/api/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company": "Anthropic",
                                  "title": "Senior Engineer",
                                  "description": "Build safe AI systems.",
                                  "jobUrl": "https://example.com/jobs/2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Anthropic"));
    }

    @Test
    void validatesJobUpdate() throws Exception {
        mockMvc.perform(put("/api/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company": "",
                                  "title": "",
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.company").value("Company is required"));
    }

    @Test
    void snoozesJobAttentionUntilFutureDate() throws Exception {
        when(jobService.snoozeAttention(
                any(Long.class),
                any(JobAttentionSnoozeRequest.class)
        )).thenReturn(jobResponse(
                1L,
                "OpenAI",
                JobStatus.APPLIED,
                LocalDate.parse("2099-08-30")
        ));

        mockMvc.perform(put("/api/jobs/1/attention-snooze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "snoozedUntil": "2099-08-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attentionSnoozedUntil").value("2099-08-30"));
    }

    @Test
    void rejectsPastAttentionSnoozeDate() throws Exception {
        mockMvc.perform(put("/api/jobs/1/attention-snooze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "snoozedUntil": "2000-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.snoozedUntil")
                        .value("Snooze date must be in the future"));
    }

    @Test
    void clearsJobAttentionSnooze() throws Exception {
        when(jobService.clearAttentionSnooze(1L))
                .thenReturn(jobResponse(1L, "OpenAI", JobStatus.APPLIED));

        mockMvc.perform(delete("/api/jobs/1/attention-snooze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attentionSnoozedUntil").doesNotExist());
    }

    @Test
    void snoozesMultipleJobReminders() throws Exception {
        when(jobService.snoozeAttention(any(JobAttentionBulkSnoozeRequest.class)))
                .thenReturn(List.of(
                        jobResponse(1L, "OpenAI", JobStatus.APPLIED, LocalDate.parse("2099-09-01")),
                        jobResponse(2L, "Anthropic", JobStatus.INTERVIEW, LocalDate.parse("2099-09-01"))
                ));

        mockMvc.perform(put("/api/jobs/attention-snooze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobIds": [1, 2],
                                  "snoozedUntil": "2099-09-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].attentionSnoozedUntil").value("2099-09-01"));
    }

    @Test
    void rejectsEmptyBulkSnoozeSelection() throws Exception {
        mockMvc.perform(put("/api/jobs/attention-snooze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobIds": [],
                                  "snoozedUntil": "2099-09-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobIds")
                        .value("At least one job is required"));
    }

    @Test
    void clearsMultipleJobReminderSnoozes() throws Exception {
        when(jobService.clearAttentionSnooze(any(JobAttentionBulkClearRequest.class)))
                .thenReturn(List.of(
                        jobResponse(1L, "OpenAI", JobStatus.APPLIED),
                        jobResponse(2L, "Anthropic", JobStatus.INTERVIEW)
                ));

        mockMvc.perform(post("/api/jobs/attention-snooze/clear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobIds": [1, 2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attentionSnoozedUntil").doesNotExist())
                .andExpect(jsonPath("$[1].attentionSnoozedUntil").doesNotExist());
    }

    @Test
    void restoresMultipleJobReminderDates() throws Exception {
        when(jobService.restoreAttentionSnoozes(any(JobAttentionBulkRestoreRequest.class)))
                .thenReturn(List.of(
                        jobResponse(1L, "OpenAI", JobStatus.APPLIED, LocalDate.parse("2099-09-01")),
                        jobResponse(2L, "Anthropic", JobStatus.INTERVIEW, LocalDate.parse("2099-09-03"))
                ));

        mockMvc.perform(put("/api/jobs/attention-snooze/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reminders": [
                                    { "jobId": 1, "snoozedUntil": "2099-09-01" },
                                    { "jobId": 2, "snoozedUntil": "2099-09-03" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attentionSnoozedUntil").value("2099-09-01"))
                .andExpect(jsonPath("$[1].attentionSnoozedUntil").value("2099-09-03"));
    }

    @Test
    void rejectsEmptyBulkReminderRestore() throws Exception {
        mockMvc.perform(put("/api/jobs/attention-snooze/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reminders": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reminders")
                        .value("At least one reminder is required"));
    }

    @Test
    void deletesJob() throws Exception {
        mockMvc.perform(delete("/api/jobs/1"))
                .andExpect(status().isNoContent());
    }

    private static JobResponse jobResponse(Long id, String company) {
        return jobResponse(id, company, JobStatus.SAVED);
    }

    private static JobResponse jobResponse(Long id, String company, JobStatus status) {
        return jobResponse(id, company, status, null);
    }

    private static JobResponse jobResponse(
            Long id,
            String company,
            JobStatus status,
            LocalDate attentionSnoozedUntil
    ) {
        return new JobResponse(
                id,
                company,
                "Software Engineer",
                "Build reliable products.",
                "https://example.com/jobs/1",
                status,
                Instant.parse("2026-08-18T12:00:00Z"),
                attentionSnoozedUntil
        );
    }
}
