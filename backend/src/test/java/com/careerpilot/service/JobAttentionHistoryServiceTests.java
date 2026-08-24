package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobAttentionHistoryResponse;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobAttentionEvent;
import com.careerpilot.model.JobAttentionEventAction;
import com.careerpilot.repository.JobAttentionEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobAttentionHistoryServiceTests {

    @Mock
    private JobAttentionEventRepository jobAttentionEventRepository;

    @InjectMocks
    private JobAttentionHistoryService jobAttentionHistoryService;

    @Test
    void returnsRecentHistoryInRepositoryOrderWithJobContext() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);
        ReflectionTestUtils.setField(job, "id", 1L);
        JobAttentionEvent event = new JobAttentionEvent(
                job,
                JobAttentionEventAction.RESCHEDULED,
                LocalDate.parse("2026-08-30"),
                LocalDate.parse("2026-09-02")
        );
        ReflectionTestUtils.setField(event, "id", 7L);
        ReflectionTestUtils.setField(event, "createdAt", Instant.parse("2026-08-24T12:00:00Z"));
        when(jobAttentionEventRepository.findTop20ByOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(event));

        List<JobAttentionHistoryResponse> responses =
                jobAttentionHistoryService.getRecentHistory();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(7L);
        assertThat(responses.getFirst().jobId()).isEqualTo(1L);
        assertThat(responses.getFirst().company()).isEqualTo("OpenAI");
        assertThat(responses.getFirst().jobTitle()).isEqualTo("Engineer");
        assertThat(responses.getFirst().action())
                .isEqualTo(JobAttentionEventAction.RESCHEDULED);
        assertThat(responses.getFirst().previousSnoozedUntil())
                .isEqualTo(LocalDate.parse("2026-08-30"));
        assertThat(responses.getFirst().newSnoozedUntil())
                .isEqualTo(LocalDate.parse("2026-09-02"));
    }
}