package com.careerpilot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.ai.AiClientException;
import com.careerpilot.dto.MatchAnalysisRequest;
import com.careerpilot.dto.MatchAnalysisResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.service.MatchAnalysisService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MatchAnalysisController.class)
class MatchAnalysisControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchAnalysisService matchAnalysisService;

    @Test
    void createsAnalysis() throws Exception {
        when(matchAnalysisService.createAnalysis(any(MatchAnalysisRequest.class)))
                .thenReturn(analysisResponse(3L));

        mockMvc.perform(post("/api/match-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 1,
                                  "resumeId": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.resumeId").value(2))
                .andExpect(jsonPath("$.matchScore").value(84))
                .andExpect(jsonPath("$.modelName").value("gpt-5.6"));
    }

    @Test
    void returnsAnalyses() throws Exception {
        when(matchAnalysisService.getAnalyses()).thenReturn(List.of(
                analysisResponse(4L),
                analysisResponse(3L)
        ));

        mockMvc.perform(get("/api/match-analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    void returnsAnalysisById() throws Exception {
        when(matchAnalysisService.getAnalysis(3L)).thenReturn(analysisResponse(3L));

        mockMvc.perform(get("/api/match-analyses/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.company").value("OpenAI"))
                .andExpect(jsonPath("$.resumeName").value("Backend Resume"));
    }

    @Test
    void returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/match-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 0,
                                  "resumeId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.jobId").value("Job id must be positive"))
                .andExpect(jsonPath("$.fieldErrors.resumeId").value("Resume id is required"));
    }

    @Test
    void returnsNotFoundError() throws Exception {
        when(matchAnalysisService.getAnalysis(99L))
                .thenThrow(new ResourceNotFoundException("Match analysis", 99L));

        mockMvc.perform(get("/api/match-analyses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match analysis not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/match-analyses/99"));
    }

    @Test
    void returnsBadGatewayWhenAiClientFails() throws Exception {
        when(matchAnalysisService.createAnalysis(any(MatchAnalysisRequest.class)))
                .thenThrow(new AiClientException("OPENAI_API_KEY is not configured"));

        mockMvc.perform(post("/api/match-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 1,
                                  "resumeId": 2
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI analysis service is temporarily unavailable"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("OPENAI_API_KEY")
                )))
                .andExpect(jsonPath("$.path").value("/api/match-analyses"));
    }

    @Test
    void deletesAnalysis() throws Exception {
        mockMvc.perform(delete("/api/match-analyses/3"))
                .andExpect(status().isNoContent());
    }

    private static MatchAnalysisResponse analysisResponse(Long id) {
        return new MatchAnalysisResponse(
                id,
                1L,
                "OpenAI",
                "Software Engineer",
                2L,
                "Backend Resume",
                84,
                "Strong overall match.",
                "Relevant backend experience.",
                "Limited cloud evidence.",
                "Add measurable cloud achievements.",
                "gpt-5.6",
                Instant.parse("2026-08-22T12:00:00Z")
        );
    }
}
