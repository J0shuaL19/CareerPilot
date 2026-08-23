package com.careerpilot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JobActivityApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobActivityRepository jobActivityRepository;

    @BeforeEach
    void clearData() {
        jobActivityRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void createsListsAndDeletesActivity() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));

        String response = mockMvc.perform(post("/api/jobs/{jobId}/activities", job.getId())
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
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long activityId = jobActivityRepository.findAll().getFirst().getId();
        assertThat(response).contains("Technical interview");

        mockMvc.perform(get("/api/jobs/{jobId}/activities", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activityId));

        mockMvc.perform(delete(
                        "/api/jobs/{jobId}/activities/{activityId}",
                        job.getId(),
                        activityId
                ))
                .andExpect(status().isNoContent());

        assertThat(jobActivityRepository.count()).isZero();
    }

    @Test
    void returnsNotFoundForMissingJob() throws Exception {
        mockMvc.perform(get("/api/jobs/999/activities"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"));
    }

    @Test
    void listsOnlyUpcomingInterviewsAndFollowUps() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));
        Instant now = Instant.now();
        jobActivityRepository.saveAllAndFlush(List.of(
                activity(job, JobActivityType.INTERVIEW, "Interview", now.plusSeconds(86_400)),
                activity(job, JobActivityType.FOLLOW_UP, "Follow-up", now.plusSeconds(172_800)),
                activity(job, JobActivityType.NOTE, "Note", now.plusSeconds(86_400)),
                activity(job, JobActivityType.INTERVIEW, "Past", now.minusSeconds(86_400))
        ));

        mockMvc.perform(get("/api/job-activities/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Interview"))
                .andExpect(jsonPath("$[1].title").value("Follow-up"));
    }

    private static JobActivity activity(
            Job job,
            JobActivityType type,
            String title,
            Instant occurredAt
    ) {
        return new JobActivity(job, type, title, null, null, occurredAt);
    }
}
