package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.InterviewPreparationRequest;
import com.careerpilot.dto.InterviewPreparationResponse;
import com.careerpilot.exception.InterviewPreparationException;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.InterviewPreparation;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.InterviewPreparationRepository;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InterviewPreparationServiceTests {

    private static final Instant NOW = Instant.parse("2026-08-24T20:00:00Z");

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActivityRepository jobActivityRepository;

    @Mock
    private InterviewPreparationRepository interviewPreparationRepository;

    private InterviewPreparationService service;

    @BeforeEach
    void setUp() {
        service = new InterviewPreparationService(
                jobRepository,
                jobActivityRepository,
                interviewPreparationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void returnsEmptyChecklistBeforeFirstSave() {
        JobActivity interview = interview(1L, 2L, JobActivityType.INTERVIEW);
        stubActivity(interview);
        when(interviewPreparationRepository.findByActivity_Id(2L))
                .thenReturn(Optional.empty());

        InterviewPreparationResponse response = service.getPreparation(1L, 2L);

        assertThat(response.activityId()).isEqualTo(2L);
        assertThat(response.completedSections()).isZero();
        assertThat(response.totalSections()).isEqualTo(4);
        assertThat(response.progressPercent()).isZero();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void savesNormalizedChecklistAndCalculatesProgress() {
        JobActivity interview = interview(1L, 2L, JobActivityType.INTERVIEW);
        stubActivity(interview);
        when(interviewPreparationRepository.findByActivity_Id(2L))
                .thenReturn(Optional.empty());
        when(interviewPreparationRepository.save(any(InterviewPreparation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InterviewPreparationResponse response = service.savePreparation(
                1L,
                2L,
                new InterviewPreparationRequest(
                        "  Product and market notes  ",
                        true,
                        "  Role priorities  ",
                        false,
                        "  Ownership STAR story  ",
                        true,
                        "   ",
                        false
                )
        );

        ArgumentCaptor<InterviewPreparation> captor =
                ArgumentCaptor.forClass(InterviewPreparation.class);
        verify(interviewPreparationRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanyResearch())
                .isEqualTo("Product and market notes");
        assertThat(captor.getValue().getQuestionsToAsk()).isNull();
        assertThat(response.completedSections()).isEqualTo(2);
        assertThat(response.progressPercent()).isEqualTo(50);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void updatesExistingChecklistInsteadOfCreatingAnother() {
        JobActivity interview = interview(1L, 2L, JobActivityType.INTERVIEW);
        InterviewPreparation existing = new InterviewPreparation(
                interview,
                Instant.parse("2026-08-20T12:00:00Z")
        );
        stubActivity(interview);
        when(interviewPreparationRepository.findByActivity_Id(2L))
                .thenReturn(Optional.of(existing));
        when(interviewPreparationRepository.save(existing)).thenReturn(existing);

        InterviewPreparationResponse response = service.savePreparation(
                1L,
                2L,
                new InterviewPreparationRequest(
                        "Company",
                        true,
                        "Role",
                        true,
                        "Stories",
                        true,
                        "Questions",
                        true
                )
        );

        assertThat(response.completedSections()).isEqualTo(4);
        assertThat(response.progressPercent()).isEqualTo(100);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsCompletedSectionWithoutNotes() {
        JobActivity interview = interview(1L, 2L, JobActivityType.INTERVIEW);
        stubActivity(interview);

        assertThatThrownBy(() -> service.savePreparation(
                1L,
                2L,
                new InterviewPreparationRequest(
                        " ",
                        true,
                        null,
                        false,
                        null,
                        false,
                        null,
                        false
                )
        ))
                .isInstanceOf(InterviewPreparationException.class)
                .hasMessage("Company research needs notes before it can be marked complete.");
    }

    @Test
    void rejectsPreparationForNonInterviewActivity() {
        JobActivity followUp = interview(1L, 2L, JobActivityType.FOLLOW_UP);
        stubActivity(followUp);

        assertThatThrownBy(() -> service.getPreparation(1L, 2L))
                .isInstanceOf(InterviewPreparationException.class)
                .hasMessage("Interview preparation is only available for interview activities.");
    }

    @Test
    void distinguishesMissingJobFromMissingActivity() {
        when(jobRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.getPreparation(9L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found with id: 9");

        when(jobRepository.existsById(1L)).thenReturn(true);
        when(jobActivityRepository.findByIdAndJob_Id(8L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPreparation(1L, 8L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job activity not found with id: 8");
    }

    private void stubActivity(JobActivity activity) {
        when(jobRepository.existsById(1L)).thenReturn(true);
        when(jobActivityRepository.findByIdAndJob_Id(2L, 1L))
                .thenReturn(Optional.of(activity));
    }

    private static JobActivity interview(
            Long jobId,
            Long activityId,
            JobActivityType type
    ) {
        Job job = new Job("OpenAI", "Engineer", "Description", null);
        ReflectionTestUtils.setField(job, "id", jobId);
        JobActivity activity = new JobActivity(
                job,
                type,
                "Technical interview",
                null,
                null,
                Instant.parse("2026-08-25T18:00:00Z")
        );
        ReflectionTestUtils.setField(activity, "id", activityId);
        return activity;
    }
}
