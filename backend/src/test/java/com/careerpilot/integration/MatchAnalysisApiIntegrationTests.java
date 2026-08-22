package com.careerpilot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.ai.AiClientException;
import com.careerpilot.ai.GeneratedMatchAnalysis;
import com.careerpilot.ai.MatchAnalysisClient;
import com.careerpilot.ai.MatchAnalysisInput;
import com.careerpilot.model.Job;
import com.careerpilot.model.MatchAnalysis;
import com.careerpilot.model.Resume;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.MatchAnalysisRepository;
import com.careerpilot.repository.ResumeRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MatchAnalysisApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MatchAnalysisRepository matchAnalysisRepository;

    @MockitoBean
    private MatchAnalysisClient matchAnalysisClient;

    @BeforeEach
    void clearData() {
        matchAnalysisRepository.deleteAll();
        resumeRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void createsPersistsListsAndReadsAnalysis() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                null
        ));
        Resume resume = resumeRepository.saveAndFlush(new Resume(
                "Backend Resume",
                "Java and Spring experience."
        ));
        when(matchAnalysisClient.analyze(any(MatchAnalysisInput.class)))
                .thenReturn(generatedAnalysis());

        mockMvc.perform(post("/api/match-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": %d,
                                  "resumeId": %d
                                }
                                """.formatted(job.getId(), resume.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andExpect(jsonPath("$.resumeId").value(resume.getId()))
                .andExpect(jsonPath("$.matchScore").value(84))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        List<MatchAnalysis> persistedAnalyses = matchAnalysisRepository.findAll();
        assertThat(persistedAnalyses).hasSize(1);
        MatchAnalysis persisted = persistedAnalyses.getFirst();
        assertThat(persisted.getModelName()).isEqualTo("gpt-5.6-test");

        mockMvc.perform(get("/api/match-analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(persisted.getId()))
                .andExpect(jsonPath("$[0].company").value("OpenAI"));

        mockMvc.perform(get("/api/match-analyses/{id}", persisted.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(persisted.getId()))
                .andExpect(jsonPath("$.summary").value("Strong overall match."));
    }

    @Test
    void returnsNotFoundWithoutCallingAi() throws Exception {
        mockMvc.perform(post("/api/match-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 999,
                                  "resumeId": 999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"));

        assertThat(matchAnalysisRepository.count()).isZero();
    }

    @Test
    void returnsBadGatewayWithoutPersistingFailedAnalysis() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));
        Resume resume = resumeRepository.saveAndFlush(new Resume("Resume", "Experience"));
        when(matchAnalysisClient.analyze(any(MatchAnalysisInput.class)))
                .thenThrow(new AiClientException("OpenAI request failed with status 500"));

        mockMvc.perform(post("/api/match-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": %d,
                                  "resumeId": %d
                                }
                                """.formatted(job.getId(), resume.getId())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI analysis service is temporarily unavailable"));

        assertThat(matchAnalysisRepository.count()).isZero();
    }

    private static GeneratedMatchAnalysis generatedAnalysis() {
        return new GeneratedMatchAnalysis(
                84,
                "Strong overall match.",
                "Relevant backend experience.",
                "Limited cloud evidence.",
                "Add measurable cloud achievements.",
                "gpt-5.6-test"
        );
    }
}
