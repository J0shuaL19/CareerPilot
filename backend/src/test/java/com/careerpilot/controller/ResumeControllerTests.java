package com.careerpilot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.ResumeRequest;
import com.careerpilot.dto.ResumeResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.service.ResumeService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumeController.class)
class ResumeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @Test
    void createsResume() throws Exception {
        when(resumeService.createResume(any(ResumeRequest.class)))
                .thenReturn(resumeResponse(1L, "Backend Resume"));

        mockMvc.perform(post("/api/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Backend Resume",
                                  "content": "Experienced Java engineer."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Backend Resume"))
                .andExpect(jsonPath("$.content").value("Experienced Java engineer."));
    }

    @Test
    void returnsResumes() throws Exception {
        when(resumeService.getResumes()).thenReturn(List.of(
                resumeResponse(2L, "Resume v2"),
                resumeResponse(1L, "Resume v1")
        ));

        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Resume v2"))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void returnsResumeById() throws Exception {
        when(resumeService.getResume(1L)).thenReturn(resumeResponse(1L, "Backend Resume"));

        mockMvc.perform(get("/api/resumes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Backend Resume"));
    }

    @Test
    void returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Resume name is required"))
                .andExpect(jsonPath("$.fieldErrors.content").value("Resume content is required"));
    }

    @Test
    void returnsNotFoundError() throws Exception {
        when(resumeService.getResume(999L))
                .thenThrow(new ResourceNotFoundException("Resume", 999L));

        mockMvc.perform(get("/api/resumes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found with id: 999"))
                .andExpect(jsonPath("$.path").value("/api/resumes/999"));
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void returnsBadRequestForInvalidIdType() throws Exception {
        mockMvc.perform(get("/api/resumes/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for id"));
    }

    private static ResumeResponse resumeResponse(Long id, String name) {
        return new ResumeResponse(
                id,
                name,
                "Experienced Java engineer.",
                Instant.parse("2026-08-21T12:00:00Z")
        );
    }
}
