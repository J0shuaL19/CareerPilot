package com.careerpilot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.InterviewPreparationRepository;
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

    @Autowired
    private InterviewPreparationRepository interviewPreparationRepository;

    @BeforeEach
    void clearData() {
        interviewPreparationRepository.deleteAll();
        jobActivityRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void createsListsUpdatesAndDeletesActivity() throws Exception {
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

        mockMvc.perform(put(
                        "/api/jobs/{jobId}/activities/{activityId}",
                        job.getId(),
                        activityId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "FOLLOW_UP",
                                  "title": "Send thank-you note",
                                  "details": "Mention platform discussion",
                                  "contact": "Alex Chen",
                                  "occurredAt": "2026-08-26T18:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.title").value("Send thank-you note"));

        JobActivity updatedActivity = jobActivityRepository.findById(activityId).orElseThrow();
        assertThat(updatedActivity.getTitle()).isEqualTo("Send thank-you note");
        assertThat(updatedActivity.getOccurredAt())
                .isEqualTo(Instant.parse("2026-08-26T18:00:00Z"));

        mockMvc.perform(get(
                        "/api/jobs/{jobId}/activities/{activityId}/calendar",
                        job.getId(),
                        activityId
                ))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo("text/calendar;charset=UTF-8"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("DTSTART:20260826T180000Z"))
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader("Content-Disposition")
                ).contains("careerpilot-activity-" + activityId + ".ics"));

        mockMvc.perform(delete(
                        "/api/jobs/{jobId}/activities/{activityId}",
                        job.getId(),
                        activityId
                ))
                .andExpect(status().isNoContent());

        assertThat(jobActivityRepository.count()).isZero();
    }

    @Test
    void savesAndReloadsInterviewPreparation() throws Exception {
        Job job = jobRepository.saveAndFlush(new Job("OpenAI", "Engineer", "Description", null));
        JobActivity interview = jobActivityRepository.saveAndFlush(activity(
                job,
                JobActivityType.INTERVIEW,
                "Technical interview",
                Instant.parse("2026-08-25T18:00:00Z")
        ));

        mockMvc.perform(get(
                        "/api/jobs/{jobId}/activities/{activityId}/preparation",
                        job.getId(),
                        interview.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSections").value(0))
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        mockMvc.perform(put(
                        "/api/jobs/{jobId}/activities/{activityId}/preparation",
                        job.getId(),
                        interview.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyResearch": "  Product and market  ",
                                  "companyResearchDone": true,
                                  "rolePriorities": "Role outcomes",
                                  "rolePrioritiesDone": true,
                                  "starStoriesDone": false,
                                  "questionsToAskDone": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyResearch").value("Product and market"))
                .andExpect(jsonPath("$.completedSections").value(2))
                .andExpect(jsonPath("$.progressPercent").value(50));

        mockMvc.perform(get(
                        "/api/jobs/{jobId}/activities/{activityId}/preparation",
                        job.getId(),
                        interview.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolePriorities").value("Role outcomes"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(interviewPreparationRepository.count()).isEqualTo(1);
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
