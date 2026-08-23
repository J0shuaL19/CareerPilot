package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobCsvImportPreviewResponse;
import com.careerpilot.dto.JobCsvImportResultResponse;
import com.careerpilot.dto.JobCsvImportRowState;
import com.careerpilot.exception.JobCsvImportException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class JobCsvImportServiceTests {

    @Mock
    private JobRepository jobRepository;

    private JobCsvImportService service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new JobCsvImportService(jobRepository, validator);
    }

    @Test
    void previewsExportedCsvWithValidDuplicateAndInvalidRows() {
        String csv = "\uFEFFID,Company,Title,Status,Job URL,Created at,Description\r\n"
                + "1,\"New Co\",\"Platform Engineer\",APPLIED,https://example.com/new,"
                + "2026-08-23T12:00:00Z,\"First line\nSecond line\"\r\n"
                + "2,Existing Co,Backend Engineer,SAVED,,,Existing role\r\n"
                + "3,new co,platform engineer,INTERVIEW,,,Repeated in file\r\n"
                + "4,,Broken Role,WAITING,not-a-url,,Description\r\n";
        when(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase("New Co", "Platform Engineer"))
                .thenReturn(false);
        when(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase("Existing Co", "Backend Engineer"))
                .thenReturn(true);
        when(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase("new co", "platform engineer"))
                .thenReturn(false);

        JobCsvImportPreviewResponse preview = service.preview(csvFile("careerpilot-jobs.csv", csv));

        assertThat(preview.filename()).isEqualTo("careerpilot-jobs.csv");
        assertThat(preview.totalRows()).isEqualTo(4);
        assertThat(preview.validRows()).isEqualTo(1);
        assertThat(preview.duplicateRows()).isEqualTo(2);
        assertThat(preview.invalidRows()).isEqualTo(1);
        assertThat(preview.rows()).extracting(row -> row.state())
                .containsExactly(
                        JobCsvImportRowState.VALID,
                        JobCsvImportRowState.DUPLICATE,
                        JobCsvImportRowState.DUPLICATE,
                        JobCsvImportRowState.INVALID
                );
        assertThat(preview.rows().getFirst().description()).isEqualTo("First line\nSecond line");
        assertThat(preview.rows().get(3).errors()).contains(
                "Company is required",
                "Job URL must be a valid URL",
                "Status must be SAVED, APPLIED, OA, INTERVIEW, OFFER, REJECTED, or WITHDRAWN"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsOnlyValidNonDuplicateRowsAndPreservesStatuses() {
        String csv = "Company,Title,Description,Job URL,Status\r\n"
                + "New Co,Engineer,Description,https://example.com/new,INTERVIEW\r\n"
                + "Another Co,Designer,Description,,\r\n"
                + "Existing Co,Engineer,Description,,APPLIED\r\n"
                + ",Invalid,Description,,SAVED\r\n";
        when(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0).equals("Existing Co"));

        JobCsvImportResultResponse result = service.importFile(csvFile("jobs.csv", csv));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skippedDuplicates()).isEqualTo(1);
        assertThat(result.skippedInvalid()).isEqualTo(1);
        ArgumentCaptor<List<Job>> jobsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jobRepository).saveAll(jobsCaptor.capture());
        assertThat(jobsCaptor.getValue()).extracting(Job::getCompany)
                .containsExactly("New Co", "Another Co");
        assertThat(jobsCaptor.getValue()).extracting(Job::getStatus)
                .containsExactly(JobStatus.INTERVIEW, JobStatus.SAVED);
    }

    @Test
    void defaultsBlankStatusAndOptionalUrl() {
        String csv = "Company,Title,Description\r\nOpenAI,Engineer,Build products\r\n";
        when(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase("OpenAI", "Engineer"))
                .thenReturn(false);

        JobCsvImportPreviewResponse preview = service.preview(csvFile("jobs.csv", csv));

        assertThat(preview.rows().getFirst().status()).isEqualTo(JobStatus.SAVED);
        assertThat(preview.rows().getFirst().jobUrl()).isNull();
        assertThat(preview.rows().getFirst().state()).isEqualTo(JobCsvImportRowState.VALID);
    }

    @Test
    void rejectsMissingRequiredHeaders() {
        assertThatThrownBy(() -> service.preview(csvFile(
                "jobs.csv",
                "Company,Title\r\nOpenAI,Engineer\r\n"
        )))
                .isInstanceOf(JobCsvImportException.class)
                .hasMessage("The CSV file must include Company, Title, and Description columns.");
    }

    @Test
    void rejectsUnclosedQuotedValue() {
        assertThatThrownBy(() -> service.preview(csvFile(
                "jobs.csv",
                "Company,Title,Description\r\nOpenAI,Engineer,\"Unclosed"
        )))
                .isInstanceOf(JobCsvImportException.class)
                .hasMessage("The CSV file contains an unclosed quoted value.");
    }

    @Test
    void rejectsUnexpectedTextAfterQuotedValue() {
        assertThatThrownBy(() -> service.preview(csvFile(
                "jobs.csv",
                "Company,Title,Description\r\n\"OpenAI\"broken,Engineer,Description\r\n"
        )))
                .isInstanceOf(JobCsvImportException.class)
                .hasMessage("The CSV file contains text after a closed quoted value.");
    }

    @Test
    void rejectsNonUtf8File() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                new byte[]{(byte) 0xC3, (byte) 0x28}
        );

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(JobCsvImportException.class)
                .hasMessage("The CSV file must use UTF-8 encoding.");
    }

    @Test
    void rejectsUnsupportedAndOversizedFiles() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "jobs.txt",
                "text/plain",
                "Company,Title,Description".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                new byte[(int) JobCsvImportService.MAX_FILE_SIZE_BYTES + 1]
        );

        assertThatThrownBy(() -> service.preview(textFile))
                .isInstanceOf(JobCsvImportException.class)
                .hasMessage("Only CSV job files are supported.");
        assertThatThrownBy(() -> service.preview(oversizedFile))
                .isInstanceOf(JobCsvImportException.class)
                .hasMessage("Job CSV files must be 2 MB or smaller.");
    }

    private static MockMultipartFile csvFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
