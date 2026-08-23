package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.careerpilot.exception.JobActivityCalendarException;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobActivityCalendarServiceTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActivityRepository jobActivityRepository;

    private JobActivityCalendarService calendarService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-23T19:00:00Z"),
                ZoneOffset.UTC
        );
        calendarService = new JobActivityCalendarService(
                jobRepository,
                jobActivityRepository,
                clock
        );
    }

    @Test
    void exportsEscapedFoldedUtf8Calendar() {
        Job job = persistedJob(1L);
        JobActivity activity = persistedActivity(
                2L,
                job,
                JobActivityType.INTERVIEW,
                "平台架构面试, final review with an intentionally long summary",
                "Discuss APIs, security; and path \\ ownership\nBring notes",
                "Alex Chen"
        );
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(2L, 1L))
                .thenReturn(Optional.of(activity));

        JobActivityCalendarFile file = calendarService.export(1L, 2L);
        String content = new String(file.content(), StandardCharsets.UTF_8);
        String unfolded = content.replace("\r\n ", "");

        assertThat(file.filename()).isEqualTo("careerpilot-activity-2.ics");
        assertThat(content).endsWith("END:VCALENDAR\r\n");
        assertThat(unfolded).contains("UID:job-1-activity-2@careerpilot.local");
        assertThat(unfolded).contains("DTSTAMP:20260823T190000Z");
        assertThat(unfolded).contains("DTSTART:20260825T180000Z");
        assertThat(unfolded).contains("SUMMARY:平台架构面试\\, final review");
        assertThat(unfolded).contains(
                "DESCRIPTION:Discuss APIs\\, security\\; and path \\\\ ownership"
                        + "\\nBring notes\\nJob: Engineer at OpenAI\\nContact: Alex Chen"
        );
        assertThat(content.split("\r\n"))
                .allSatisfy(line -> assertThat(line.getBytes(StandardCharsets.UTF_8).length)
                        .isLessThanOrEqualTo(75));
    }

    @Test
    void rejectsNonReminderActivity() {
        Job job = persistedJob(1L);
        JobActivity activity = persistedActivity(
                2L,
                job,
                JobActivityType.NOTE,
                "Research note",
                null,
                null
        );
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(2L, 1L))
                .thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> calendarService.export(1L, 2L))
                .isInstanceOf(JobActivityCalendarException.class)
                .hasMessage("Only interviews and follow-ups can be exported to a calendar.");
    }

    @Test
    void rejectsActivityOwnedByAnotherJob() {
        Job job = persistedJob(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.export(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job activity not found with id: 99");
    }

    private static Job persistedJob(Long id) {
        Job job = new Job("OpenAI", "Engineer", "Description", null);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private static JobActivity persistedActivity(
            Long id,
            Job job,
            JobActivityType type,
            String title,
            String details,
            String contact
    ) {
        JobActivity activity = new JobActivity(
                job,
                type,
                title,
                details,
                contact,
                Instant.parse("2026-08-25T18:00:00Z")
        );
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }
}
