package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobCsvExportServiceTests {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobCsvExportService jobCsvExportService;

    @Test
    void exportsJobsInRequestedOrderWithUtf8BomAndCrLfRows() {
        Job firstRequested = persistedJob(
                2L,
                "星河科技",
                "Senior \"Platform\" Engineer",
                "Build systems, safely.\nWork across teams.",
                "https://example.com/jobs/2",
                JobStatus.INTERVIEW,
                "2026-08-19T12:30:00Z"
        );
        Job secondRequested = persistedJob(
                1L,
                "Company A",
                "Engineer",
                "Description",
                null,
                JobStatus.SAVED,
                "2026-08-18T12:30:00Z"
        );
        when(jobRepository.findAllById(List.of(2L, 1L)))
                .thenReturn(List.of(secondRequested, firstRequested));

        JobCsvFile file = jobCsvExportService.export(List.of(2L, 1L));

        assertThat(file.filename()).isEqualTo("careerpilot-jobs.csv");
        assertThat(file.content()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(file.content(), StandardCharsets.UTF_8)).isEqualTo(
                "\uFEFFID,Company,Title,Status,Job URL,Created at,Description\r\n"
                        + "2,\"星河科技\",\"Senior \"\"Platform\"\" Engineer\",INTERVIEW,"
                        + "\"https://example.com/jobs/2\",2026-08-19T12:30:00Z,"
                        + "\"Build systems, safely.\nWork across teams.\"\r\n"
                        + "1,\"Company A\",\"Engineer\",SAVED,\"\",2026-08-18T12:30:00Z,"
                        + "\"Description\"\r\n"
        );
    }

    @Test
    void neutralizesValuesThatSpreadsheetAppsCouldTreatAsFormulas() {
        Job job = persistedJob(
                1L,
                "=DANGEROUS()",
                "  +SUM(1,2)",
                "@command",
                "-1+1",
                JobStatus.APPLIED,
                "2026-08-18T12:30:00Z"
        );
        when(jobRepository.findAllById(List.of(1L))).thenReturn(List.of(job));

        String csv = new String(jobCsvExportService.export(List.of(1L)).content(), StandardCharsets.UTF_8);

        assertThat(csv).contains(
                "\"'=DANGEROUS()\"",
                "\"'  +SUM(1,2)\"",
                "\"'@command\"",
                "\"'-1+1\""
        );
    }

    @Test
    void removesDuplicateIdsWhilePreservingFirstOccurrence() {
        Job job = persistedJob(
                1L,
                "Company A",
                "Engineer",
                "Description",
                null,
                JobStatus.SAVED,
                "2026-08-18T12:30:00Z"
        );
        when(jobRepository.findAllById(List.of(1L))).thenReturn(List.of(job));

        String csv = new String(
                jobCsvExportService.export(List.of(1L, 1L)).content(),
                StandardCharsets.UTF_8
        );

        assertThat(csv.lines().filter(line -> line.startsWith("1,")).count()).isEqualTo(1);
    }

    @Test
    void throwsWhenARequestedJobNoLongerExists() {
        Job job = persistedJob(
                1L,
                "Company A",
                "Engineer",
                "Description",
                null,
                JobStatus.SAVED,
                "2026-08-18T12:30:00Z"
        );
        when(jobRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(job));

        assertThatThrownBy(() -> jobCsvExportService.export(List.of(1L, 999L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found with id: 999");
    }

    private static Job persistedJob(
            Long id,
            String company,
            String title,
            String description,
            String jobUrl,
            JobStatus status,
            String createdAt
    ) {
        Job job = new Job(company, title, description, jobUrl);
        job.updateStatus(status);
        ReflectionTestUtils.setField(job, "id", id);
        ReflectionTestUtils.setField(job, "createdAt", Instant.parse(createdAt));
        return job;
    }
}
