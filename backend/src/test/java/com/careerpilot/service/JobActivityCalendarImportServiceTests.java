package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobActivityCalendarImportItemRequest;
import com.careerpilot.dto.JobActivityCalendarImportPreviewResponse;
import com.careerpilot.dto.JobActivityCalendarImportResultResponse;
import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.exception.JobActivityCalendarImportException;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.JobActivityRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class JobActivityCalendarImportServiceTests {

    @Mock
    private JobActivityRepository jobActivityRepository;

    @Mock
    private JobActivityService jobActivityService;

    private JobActivityCalendarImportService service;

    @BeforeEach
    void setUp() {
        service = new JobActivityCalendarImportService(
                jobActivityRepository,
                jobActivityService,
                Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void previewsTimezoneFoldedTextAllDayAndInvalidEvents() {
        String ics = """
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:interview-1
                DTSTART;TZID=America/Los_Angeles:20260825T090000
                SUMMARY:Technical interview\\, platform
                DESCRIPTION:Panel with Alice\\nBring system design notes and
                 examples
                LOCATION:Zoom\\; Room 4
                END:VEVENT
                BEGIN:VEVENT
                DTSTART;VALUE=DATE:20260827
                SUMMARY:Recruiter follow-up
                END:VEVENT
                BEGIN:VEVENT
                SUMMARY:Missing time
                END:VEVENT
                END:VCALENDAR
                """;

        JobActivityCalendarImportPreviewResponse preview = service.preview(
                calendarFile("external.ics", ics)
        );

        assertThat(preview.filename()).isEqualTo("external.ics");
        assertThat(preview.totalEvents()).isEqualTo(3);
        assertThat(preview.importableEvents()).isEqualTo(2);
        assertThat(preview.invalidEvents()).isEqualTo(1);
        assertThat(preview.events().getFirst().occurredAt())
                .isEqualTo(Instant.parse("2026-08-25T16:00:00Z"));
        assertThat(preview.events().getFirst().title())
                .isEqualTo("Technical interview, platform");
        assertThat(preview.events().getFirst().details())
                .isEqualTo("Panel with Alice\nBring system design notes andexamples");
        assertThat(preview.events().getFirst().contact()).isEqualTo("Zoom; Room 4");
        assertThat(preview.events().getFirst().suggestedType())
                .isEqualTo(JobActivityType.INTERVIEW);
        assertThat(preview.events().get(1).occurredAt())
                .isEqualTo(Instant.parse("2026-08-27T00:00:00Z"));
        assertThat(preview.events().get(1).suggestedType())
                .isEqualTo(JobActivityType.FOLLOW_UP);
        assertThat(preview.events().get(2).importable()).isFalse();
        assertThat(preview.events().get(2).errors())
                .containsExactly("Event start time is required");
    }

    @Test
    void reportsUnsupportedTimezoneWithoutRejectingOtherEvents() {
        String ics = """
                BEGIN:VCALENDAR
                BEGIN:VEVENT
                DTSTART;TZID=Mars/Olympus:20260825T090000
                SUMMARY:Remote interview
                END:VEVENT
                BEGIN:VEVENT
                DTSTART:20260825T180000Z
                SUMMARY:Valid interview
                END:VEVENT
                END:VCALENDAR
                """;

        JobActivityCalendarImportPreviewResponse preview = service.preview(
                calendarFile("calendar.ics", ics)
        );

        assertThat(preview.importableEvents()).isEqualTo(1);
        assertThat(preview.invalidEvents()).isEqualTo(1);
        assertThat(preview.events().getFirst().errors())
                .containsExactly("Event start time or timezone is not supported");
    }

    @Test
    void importsUniqueEventsAndSkipsRequestAndDatabaseDuplicates() {
        JobActivityCalendarImportItemRequest first = event(
                4L,
                JobActivityType.INTERVIEW,
                "Technical interview",
                "2026-08-25T16:00:00Z"
        );
        JobActivityCalendarImportItemRequest duplicateInRequest = event(
                4L,
                JobActivityType.INTERVIEW,
                "technical interview",
                "2026-08-25T16:00:00Z"
        );
        JobActivityCalendarImportItemRequest duplicateInDatabase = event(
                7L,
                JobActivityType.FOLLOW_UP,
                "Recruiter follow-up",
                "2026-08-28T17:00:00Z"
        );
        when(jobActivityRepository.existsByJob_IdAndTypeAndTitleIgnoreCaseAndOccurredAt(
                anyLong(),
                any(JobActivityType.class),
                anyString(),
                any(Instant.class)
        )).thenAnswer(invocation -> invocation.getArgument(0).equals(7L));

        JobActivityCalendarImportResultResponse result = service.importEvents(
                List.of(first, duplicateInRequest, duplicateInDatabase)
        );

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skippedDuplicates()).isEqualTo(2);
        ArgumentCaptor<JobActivityRequest> requestCaptor =
                ArgumentCaptor.forClass(JobActivityRequest.class);
        verify(jobActivityService).createActivity(
                org.mockito.ArgumentMatchers.eq(4L),
                requestCaptor.capture()
        );
        assertThat(requestCaptor.getValue().title()).isEqualTo("Technical interview");
        assertThat(requestCaptor.getValue().details()).isEqualTo("Agenda");
        assertThat(requestCaptor.getValue().contact()).isEqualTo("Zoom");
    }

    @Test
    void rejectsActivityTypesOutsideCalendarScope() {
        JobActivityCalendarImportItemRequest note = event(
                4L,
                JobActivityType.NOTE,
                "Private note",
                "2026-08-25T16:00:00Z"
        );

        assertThatThrownBy(() -> service.importEvents(List.of(note)))
                .isInstanceOf(JobActivityCalendarImportException.class)
                .hasMessage("Calendar imports only support interviews and follow-ups.");
    }

    @Test
    void rejectsUnsupportedEmptyOversizedAndNonUtf8Files() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "calendar.txt",
                "text/plain",
                "BEGIN:VCALENDAR".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile emptyCalendar = calendarFile(
                "empty.ics",
                "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"
        );
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "large.ics",
                "text/calendar",
                new byte[(int) JobActivityCalendarImportService.MAX_FILE_SIZE_BYTES + 1]
        );
        MockMultipartFile nonUtf8File = new MockMultipartFile(
                "file",
                "broken.ics",
                "text/calendar",
                new byte[]{(byte) 0xC3, (byte) 0x28}
        );

        assertThatThrownBy(() -> service.preview(textFile))
                .isInstanceOf(JobActivityCalendarImportException.class)
                .hasMessage("Only ICS calendar files are supported.");
        assertThatThrownBy(() -> service.preview(emptyCalendar))
                .isInstanceOf(JobActivityCalendarImportException.class)
                .hasMessage("The calendar file does not contain any events.");
        assertThatThrownBy(() -> service.preview(oversizedFile))
                .isInstanceOf(JobActivityCalendarImportException.class)
                .hasMessage("Calendar files must be 1 MB or smaller.");
        assertThatThrownBy(() -> service.preview(nonUtf8File))
                .isInstanceOf(JobActivityCalendarImportException.class)
                .hasMessage("The calendar file must use UTF-8 encoding.");
    }

    private static JobActivityCalendarImportItemRequest event(
            Long jobId,
            JobActivityType type,
            String title,
            String occurredAt
    ) {
        return new JobActivityCalendarImportItemRequest(
                jobId,
                type,
                title,
                " Agenda ",
                " Zoom ",
                Instant.parse(occurredAt)
        );
    }

    private static MockMultipartFile calendarFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/calendar",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
