package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerpilot.dto.CareerPilotArchive;
import com.careerpilot.dto.DataTransferImportResponse;
import com.careerpilot.dto.DataTransferPreviewResponse;
import com.careerpilot.exception.DataTransferException;
import com.careerpilot.exception.DataTransferImportDisabledException;
import com.careerpilot.model.InterviewPreparation;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.model.JobAttentionEvent;
import com.careerpilot.model.JobAttentionEventAction;
import com.careerpilot.model.JobAttentionSettings;
import com.careerpilot.model.JobStatus;
import com.careerpilot.model.MatchAnalysis;
import com.careerpilot.model.Resume;
import com.careerpilot.repository.InterviewPreparationRepository;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobAttentionEventRepository;
import com.careerpilot.repository.JobAttentionSettingsRepository;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.MatchAnalysisRepository;
import com.careerpilot.repository.ResumeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.web.MockMultipartFile;

@DataJpaTest
class DataTransferServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-08T20:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-06-01T10:15:30Z");

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobActivityRepository jobActivityRepository;

    @Autowired
    private MatchAnalysisRepository matchAnalysisRepository;

    @Autowired
    private JobAttentionSettingsRepository attentionSettingsRepository;

    @Autowired
    private JobAttentionEventRepository attentionEventRepository;

    @Autowired
    private InterviewPreparationRepository interviewPreparationRepository;

    @Autowired
    private EntityManager entityManager;

    private ObjectMapper objectMapper;
    private DataTransferService service;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        service = createService(true);
    }

    @Test
    void exportsPreviewsAndRestoresEveryBusinessRecord() throws Exception {
        createCompleteDataset();
        DataTransferFile exported = service.exportData();
        CareerPilotArchive archive = objectMapper.readValue(
                exported.content(),
                CareerPilotArchive.class
        );

        assertThat(exported.filename()).matches("careerpilot-data-\\d{8}-\\d{6}\\.json");
        assertThat(archive.format()).isEqualTo("careerpilot-data");
        assertThat(archive.version()).isEqualTo(1);
        assertThat(archive.exportedAt()).isEqualTo(NOW);
        assertThat(archive.data().jobs()).hasSize(1);
        assertThat(archive.data().resumes()).hasSize(1);
        assertThat(archive.data().jobActivities()).hasSize(1);
        assertThat(archive.data().matchAnalyses()).hasSize(1);
        assertThat(archive.data().attentionEvents()).hasSize(1);
        assertThat(archive.data().interviewPreparations()).hasSize(1);

        jobRepository.saveAndFlush(new Job("Temporary", "Delete me", "Local data", null));
        MockMultipartFile file = archiveFile(exported.content());
        DataTransferPreviewResponse preview = service.previewImport(file);

        assertThat(preview.incoming().totalRecords()).isEqualTo(7);
        assertThat(preview.existing().jobs()).isEqualTo(2);
        assertThat(preview.willReplaceExistingData()).isTrue();

        DataTransferImportResponse result = service.importData(file);
        entityManager.clear();

        assertThat(result.importedAt()).isEqualTo(NOW);
        assertThat(result.imported().totalRecords()).isEqualTo(7);
        assertThat(jobRepository.count()).isEqualTo(1);
        assertThat(resumeRepository.count()).isEqualTo(1);
        assertThat(jobActivityRepository.count()).isEqualTo(1);
        assertThat(matchAnalysisRepository.count()).isEqualTo(1);
        assertThat(attentionEventRepository.count()).isEqualTo(1);
        assertThat(interviewPreparationRepository.count()).isEqualTo(1);

        Job restoredJob = jobRepository.findAll().getFirst();
        JobActivity restoredActivity = jobActivityRepository.findAll().getFirst();
        MatchAnalysis restoredAnalysis = matchAnalysisRepository.findAllByOrderByCreatedAtDesc()
                .getFirst();
        InterviewPreparation restoredPreparation = interviewPreparationRepository.findAll()
                .getFirst();

        assertThat(restoredJob.getCompany()).isEqualTo("OpenAI");
        assertThat(restoredJob.getStatus()).isEqualTo(JobStatus.INTERVIEW);
        assertThat(restoredJob.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(restoredJob.getAttentionSnoozedUntil()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(restoredActivity.getJob().getId()).isEqualTo(restoredJob.getId());
        assertThat(restoredActivity.getCompletedAt())
                .isEqualTo(Instant.parse("2026-06-03T18:00:00Z"));
        assertThat(restoredAnalysis.getJob().getId()).isEqualTo(restoredJob.getId());
        assertThat(restoredAnalysis.getResume().getName()).isEqualTo("Backend resume");
        assertThat(restoredPreparation.getActivity().getId()).isEqualTo(restoredActivity.getId());
        assertThat(restoredPreparation.isCompanyResearchDone()).isTrue();
        assertThat(attentionSettingsRepository.findById(1L).orElseThrow().getAppliedDays())
                .isEqualTo(9);
    }

    @Test
    void rejectsBrokenReferencesBeforeDeletingLocalData() throws Exception {
        createCompleteDataset();
        DataTransferFile exported = service.exportData();
        JsonNode root = objectMapper.readTree(exported.content());
        ((com.fasterxml.jackson.databind.node.ObjectNode) root
                .path("data")
                .path("jobActivities")
                .get(0))
                .put("jobSourceId", 9999);
        MockMultipartFile broken = archiveFile(objectMapper.writeValueAsBytes(root));

        assertThatThrownBy(() -> service.importData(broken))
                .isInstanceOf(DataTransferException.class)
                .hasMessage("A job activity refers to a missing job.");

        assertThat(jobRepository.count()).isEqualTo(1);
        assertThat(jobActivityRepository.count()).isEqualTo(1);
    }

    @Test
    void keepsFullImportDisabledOutsideTheDesktopProfile() {
        DataTransferService webService = createService(false);
        MockMultipartFile file = archiveFile("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> webService.previewImport(file))
                .isInstanceOf(DataTransferImportDisabledException.class)
                .hasMessage("Full data import is only available in the CareerPilot Windows app.");
    }

    private void createCompleteDataset() {
        Job job = jobRepository.saveAndFlush(Job.restore(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                "https://example.com/jobs/1",
                JobStatus.INTERVIEW,
                CREATED_AT,
                LocalDate.of(2026, 6, 10)
        ));
        Resume resume = resumeRepository.saveAndFlush(Resume.restore(
                "Backend resume",
                "Java, Spring Boot, PostgreSQL",
                CREATED_AT.plusSeconds(60)
        ));
        JobActivity activity = jobActivityRepository.saveAndFlush(JobActivity.restore(
                job,
                JobActivityType.INTERVIEW,
                "Technical interview",
                "System design and APIs",
                "Hiring team",
                Instant.parse("2026-06-03T17:00:00Z"),
                Instant.parse("2026-06-03T18:00:00Z"),
                "Completed",
                JobStatus.APPLIED,
                JobStatus.INTERVIEW,
                CREATED_AT.plusSeconds(120)
        ));
        matchAnalysisRepository.saveAndFlush(MatchAnalysis.restore(
                job,
                resume,
                88,
                "Strong fit",
                "Backend experience",
                "Limited cloud detail",
                "Add deployment examples",
                "test-model",
                CREATED_AT.plusSeconds(180)
        ));
        attentionEventRepository.saveAndFlush(JobAttentionEvent.restore(
                job,
                JobAttentionEventAction.SNOOZED,
                null,
                LocalDate.of(2026, 6, 10),
                CREATED_AT.plusSeconds(240)
        ));
        JobAttentionSettings settings = attentionSettingsRepository.findById(1L).orElseThrow();
        settings.update(9, 8, 6, CREATED_AT.plusSeconds(300));
        attentionSettingsRepository.saveAndFlush(settings);
        interviewPreparationRepository.saveAndFlush(InterviewPreparation.restore(
                activity,
                "Product research",
                true,
                "Role priorities",
                false,
                "STAR stories",
                true,
                "Questions",
                false,
                CREATED_AT.plusSeconds(360)
        ));
        entityManager.clear();
    }

    private DataTransferService createService(boolean importEnabled) {
        return new DataTransferService(
                jobRepository,
                resumeRepository,
                jobActivityRepository,
                matchAnalysisRepository,
                attentionSettingsRepository,
                attentionEventRepository,
                interviewPreparationRepository,
                objectMapper,
                entityManager,
                Clock.fixed(NOW, ZoneOffset.UTC),
                importEnabled
        );
    }

    private static MockMultipartFile archiveFile(byte[] content) {
        return new MockMultipartFile(
                "file",
                "careerpilot-data.json",
                "application/json",
                content
        );
    }
}
