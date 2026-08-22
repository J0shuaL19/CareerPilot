package com.careerpilot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JobApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void clearJobs() {
        jobRepository.deleteAll();
    }

    @Test
    void createsPersistsAndReadsJob() throws Exception {
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
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        List<Job> persistedJobs = jobRepository.findAll();
        assertThat(persistedJobs).hasSize(1);
        Job persistedJob = persistedJobs.getFirst();
        assertThat(persistedJob.getCompany()).isEqualTo("OpenAI");

        mockMvc.perform(get("/api/jobs/{id}", persistedJob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(persistedJob.getId()))
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    void listsJobsNewestFirst() throws Exception {
        Job olderJob = job("Company A", "2026-08-17T12:00:00Z");
        Job newestJob = job("Company B", "2026-08-18T12:00:00Z");
        jobRepository.saveAllAndFlush(List.of(olderJob, newestJob));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].company").value("Company B"))
                .andExpect(jsonPath("$[1].company").value("Company A"));
    }

    @Test
    void rejectsInvalidRequestWithoutPersistingJob() throws Exception {
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
                .andExpect(jsonPath("$.fieldErrors.company").exists());

        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void updatesAndPersistsJobStatus() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));

        mockMvc.perform(patch("/api/jobs/{id}/status", job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INTERVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.INTERVIEW);
    }

    @Test
    void updatesAndPersistsJobDetails() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));

        mockMvc.perform(put("/api/jobs/{id}", job.getId())
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

        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getTitle()).isEqualTo("Senior Engineer");
        assertThat(updatedJob.getJobUrl()).isEqualTo("https://example.com/jobs/2");
    }

    @Test
    void deletesJob() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));

        mockMvc.perform(delete("/api/jobs/{id}", job.getId()))
                .andExpect(status().isNoContent());

        assertThat(jobRepository.existsById(job.getId())).isFalse();
    }

    private static Job job(String company, String createdAt) {
        Job job = new Job(company, "Engineer", "Description", null);
        ReflectionTestUtils.setField(job, "createdAt", Instant.parse(createdAt));
        return job;
    }
}
