package com.careerpilot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.JobStatus;
import com.careerpilot.service.JobService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobController.class)
class JobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

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

    private static JobResponse jobResponse(Long id, String company) {
        return new JobResponse(
                id,
                company,
                "Software Engineer",
                "Build reliable products.",
                "https://example.com/jobs/1",
                JobStatus.SAVED,
                Instant.parse("2026-08-18T12:00:00Z")
        );
    }
}
