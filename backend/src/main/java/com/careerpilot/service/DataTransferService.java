package com.careerpilot.service;

import com.careerpilot.dto.CareerPilotArchive;
import com.careerpilot.dto.CareerPilotArchive.ArchiveData;
import com.careerpilot.dto.CareerPilotArchive.AttentionEventItem;
import com.careerpilot.dto.CareerPilotArchive.AttentionSettingsItem;
import com.careerpilot.dto.CareerPilotArchive.InterviewPreparationItem;
import com.careerpilot.dto.CareerPilotArchive.JobActivityItem;
import com.careerpilot.dto.CareerPilotArchive.JobItem;
import com.careerpilot.dto.CareerPilotArchive.MatchAnalysisItem;
import com.careerpilot.dto.CareerPilotArchive.ResumeItem;
import com.careerpilot.dto.DataTransferCapabilitiesResponse;
import com.careerpilot.dto.DataTransferCountsResponse;
import com.careerpilot.dto.DataTransferImportResponse;
import com.careerpilot.dto.DataTransferPreviewResponse;
import com.careerpilot.exception.DataTransferException;
import com.careerpilot.exception.DataTransferImportDisabledException;
import com.careerpilot.model.InterviewPreparation;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.model.JobAttentionEvent;
import com.careerpilot.model.JobAttentionSettings;
import com.careerpilot.model.MatchAnalysis;
import com.careerpilot.model.Resume;
import com.careerpilot.repository.InterviewPreparationRepository;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobAttentionEventRepository;
import com.careerpilot.repository.JobAttentionSettingsRepository;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.MatchAnalysisRepository;
import com.careerpilot.repository.ResumeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DataTransferService {

    static final String ARCHIVE_FORMAT = "careerpilot-data";
    static final int FORMAT_VERSION = 1;
    static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_SECTION_RECORDS = 20_000;
    private static final Long ATTENTION_SETTINGS_ID = 1L;
    private static final DateTimeFormatter FILENAME_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final JobActivityRepository jobActivityRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final JobAttentionSettingsRepository attentionSettingsRepository;
    private final JobAttentionEventRepository attentionEventRepository;
    private final InterviewPreparationRepository interviewPreparationRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final Clock clock;
    private final boolean importEnabled;

    public DataTransferService(
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            JobActivityRepository jobActivityRepository,
            MatchAnalysisRepository matchAnalysisRepository,
            JobAttentionSettingsRepository attentionSettingsRepository,
            JobAttentionEventRepository attentionEventRepository,
            InterviewPreparationRepository interviewPreparationRepository,
            ObjectMapper objectMapper,
            EntityManager entityManager,
            Clock clock,
            @Value("${careerpilot.data-transfer.import-enabled:false}") boolean importEnabled
    ) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.matchAnalysisRepository = matchAnalysisRepository;
        this.attentionSettingsRepository = attentionSettingsRepository;
        this.attentionEventRepository = attentionEventRepository;
        this.interviewPreparationRepository = interviewPreparationRepository;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.clock = clock;
        this.importEnabled = importEnabled;
    }

    public DataTransferCapabilitiesResponse capabilities() {
        return new DataTransferCapabilitiesResponse(
                importEnabled,
                FORMAT_VERSION,
                MAX_FILE_SIZE_BYTES
        );
    }

    @Transactional(readOnly = true)
    public DataTransferFile exportData() {
        Instant exportedAt = clock.instant();
        ArchiveData data = new ArchiveData(
                jobRepository.findAll(Sort.by("id")).stream()
                        .map(DataTransferService::toJobItem)
                        .toList(),
                resumeRepository.findAll(Sort.by("id")).stream()
                        .map(DataTransferService::toResumeItem)
                        .toList(),
                jobActivityRepository.findAll(Sort.by("id")).stream()
                        .map(DataTransferService::toJobActivityItem)
                        .toList(),
                matchAnalysisRepository.findAll(Sort.by("id")).stream()
                        .map(DataTransferService::toMatchAnalysisItem)
                        .toList(),
                attentionSettingsRepository.findById(ATTENTION_SETTINGS_ID)
                        .map(DataTransferService::toAttentionSettingsItem)
                        .orElseThrow(() -> new DataTransferException(
                                "Reminder settings are missing and cannot be exported."
                        )),
                attentionEventRepository.findAll(Sort.by("id")).stream()
                        .map(DataTransferService::toAttentionEventItem)
                        .toList(),
                interviewPreparationRepository.findAll(Sort.by("id")).stream()
                        .map(DataTransferService::toInterviewPreparationItem)
                        .toList()
        );
        CareerPilotArchive archive = new CareerPilotArchive(
                ARCHIVE_FORMAT,
                FORMAT_VERSION,
                exportedAt,
                data
        );

        try {
            byte[] content = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(archive);
            return new DataTransferFile(
                    "careerpilot-data-" + FILENAME_TIME.format(exportedAt) + ".json",
                    content
            );
        } catch (JsonProcessingException exception) {
            throw new DataTransferException("CareerPilot could not create the data export.", exception);
        }
    }

    @Transactional(readOnly = true)
    public DataTransferPreviewResponse previewImport(MultipartFile file) {
        ensureImportEnabled();
        ParsedArchive parsed = parse(file);
        DataTransferCountsResponse incoming = counts(parsed.archive().data());
        DataTransferCountsResponse existing = existingCounts();
        return new DataTransferPreviewResponse(
                parsed.filename(),
                FORMAT_VERSION,
                parsed.archive().exportedAt(),
                incoming,
                existing,
                existing.totalRecords() > 0
        );
    }

    @Transactional
    public DataTransferImportResponse importData(MultipartFile file) {
        ensureImportEnabled();
        CareerPilotArchive archive = parse(file).archive();
        ArchiveData data = archive.data();
        deleteExistingData();

        Map<Long, Job> jobs = new HashMap<>();
        for (JobItem item : data.jobs()) {
            Job job = jobRepository.save(Job.restore(
                    item.company(),
                    item.title(),
                    item.description(),
                    item.jobUrl(),
                    item.status(),
                    item.createdAt(),
                    item.attentionSnoozedUntil()
            ));
            jobs.put(item.sourceId(), job);
        }

        Map<Long, Resume> resumes = new HashMap<>();
        for (ResumeItem item : data.resumes()) {
            Resume resume = resumeRepository.save(Resume.restore(
                    item.name(),
                    item.content(),
                    item.createdAt()
            ));
            resumes.put(item.sourceId(), resume);
        }

        Map<Long, JobActivity> activities = new HashMap<>();
        for (JobActivityItem item : data.jobActivities()) {
            JobActivity activity = jobActivityRepository.save(JobActivity.restore(
                    jobs.get(item.jobSourceId()),
                    item.type(),
                    item.title(),
                    item.details(),
                    item.contact(),
                    item.occurredAt(),
                    item.completedAt(),
                    item.completionNote(),
                    item.completionPreviousJobStatus(),
                    item.completionAppliedJobStatus(),
                    item.createdAt()
            ));
            activities.put(item.sourceId(), activity);
        }

        for (MatchAnalysisItem item : data.matchAnalyses()) {
            matchAnalysisRepository.save(MatchAnalysis.restore(
                    jobs.get(item.jobSourceId()),
                    resumes.get(item.resumeSourceId()),
                    item.matchScore(),
                    item.summary(),
                    item.strengths(),
                    item.gaps(),
                    item.recommendations(),
                    item.modelName(),
                    item.createdAt()
            ));
        }

        AttentionSettingsItem settings = data.attentionSettings();
        attentionSettingsRepository.save(new JobAttentionSettings(
                ATTENTION_SETTINGS_ID,
                settings.appliedDays(),
                settings.onlineAssessmentDays(),
                settings.interviewDays(),
                settings.updatedAt()
        ));

        for (AttentionEventItem item : data.attentionEvents()) {
            attentionEventRepository.save(JobAttentionEvent.restore(
                    jobs.get(item.jobSourceId()),
                    item.action(),
                    item.previousSnoozedUntil(),
                    item.newSnoozedUntil(),
                    item.createdAt()
            ));
        }

        for (InterviewPreparationItem item : data.interviewPreparations()) {
            interviewPreparationRepository.save(InterviewPreparation.restore(
                    activities.get(item.activitySourceId()),
                    item.companyResearch(),
                    item.companyResearchDone(),
                    item.rolePriorities(),
                    item.rolePrioritiesDone(),
                    item.starStories(),
                    item.starStoriesDone(),
                    item.questionsToAsk(),
                    item.questionsToAskDone(),
                    item.updatedAt()
            ));
        }

        entityManager.flush();
        return new DataTransferImportResponse(clock.instant(), counts(data));
    }

    private void deleteExistingData() {
        interviewPreparationRepository.deleteAllInBatch();
        matchAnalysisRepository.deleteAllInBatch();
        attentionEventRepository.deleteAllInBatch();
        jobActivityRepository.deleteAllInBatch();
        attentionSettingsRepository.deleteAllInBatch();
        resumeRepository.deleteAllInBatch();
        jobRepository.deleteAllInBatch();
        entityManager.clear();
    }

    private ParsedArchive parse(MultipartFile file) {
        String filename = validateFile(file);
        try {
            CareerPilotArchive archive = objectMapper.readValue(
                    file.getBytes(),
                    CareerPilotArchive.class
            );
            validateArchive(archive);
            return new ParsedArchive(filename, archive);
        } catch (DataTransferException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new DataTransferException(
                    "This is not a valid CareerPilot data export.",
                    exception
            );
        }
    }

    private void validateArchive(CareerPilotArchive archive) {
        require(archive != null, "The data export is empty.");
        require(ARCHIVE_FORMAT.equals(archive.format()), "Unrecognized data export format.");
        require(archive.version() != null, "The data export version is missing.");
        require(
                archive.version() == FORMAT_VERSION,
                "This data export version is not supported by this CareerPilot app."
        );
        require(archive.exportedAt() != null, "The data export date is missing.");
        require(archive.data() != null, "The data export content is missing.");
        ArchiveData data = archive.data();
        requireLists(data);

        Set<Long> jobIds = validateSourceIds("jobs", data.jobs(), JobItem::sourceId);
        Set<Long> resumeIds = validateSourceIds("resumes", data.resumes(), ResumeItem::sourceId);
        Set<Long> activityIds = validateSourceIds(
                "job activities",
                data.jobActivities(),
                JobActivityItem::sourceId
        );
        validateSourceIds("match analyses", data.matchAnalyses(), MatchAnalysisItem::sourceId);
        validateSourceIds("attention events", data.attentionEvents(), AttentionEventItem::sourceId);
        validateSourceIds(
                "interview preparations",
                data.interviewPreparations(),
                InterviewPreparationItem::sourceId
        );

        for (JobItem item : data.jobs()) {
            requireText(item.company(), "A job company is missing.", 255);
            requireText(item.title(), "A job title is missing.", 255);
            requireText(item.description(), "A job description is missing.", null);
            requireLength(item.jobUrl(), 2048, "A job URL is too long.");
            require(item.status() != null, "A job status is missing.");
            require(item.createdAt() != null, "A job creation date is missing.");
        }
        for (ResumeItem item : data.resumes()) {
            requireText(item.name(), "A resume name is missing.", 255);
            requireText(item.content(), "Resume content is missing.", null);
            require(item.createdAt() != null, "A resume creation date is missing.");
        }

        Map<Long, JobActivityItem> activityById = new HashMap<>();
        for (JobActivityItem item : data.jobActivities()) {
            require(jobIds.contains(item.jobSourceId()), "A job activity refers to a missing job.");
            require(item.type() != null, "A job activity type is missing.");
            requireText(item.title(), "A job activity title is missing.", 255);
            requireLength(item.details(), 5000, "Job activity details are too long.");
            requireLength(item.contact(), 255, "A job activity contact is too long.");
            require(item.occurredAt() != null, "A job activity date is missing.");
            require(item.createdAt() != null, "A job activity creation date is missing.");
            requireLength(item.completionNote(), 2000, "A completion note is too long.");
            require(
                    (item.completionPreviousJobStatus() == null)
                            == (item.completionAppliedJobStatus() == null),
                    "A job activity has incomplete status history."
            );
            if (item.completedAt() == null) {
                require(
                        item.completionNote() == null
                                && item.completionPreviousJobStatus() == null,
                        "An incomplete job activity contains completion data."
                );
            }
            activityById.put(item.sourceId(), item);
        }

        for (MatchAnalysisItem item : data.matchAnalyses()) {
            require(jobIds.contains(item.jobSourceId()), "A match analysis refers to a missing job.");
            require(
                    resumeIds.contains(item.resumeSourceId()),
                    "A match analysis refers to a missing resume."
            );
            require(
                    item.matchScore() >= 0 && item.matchScore() <= 100,
                    "A match analysis score must be between 0 and 100."
            );
            requireText(item.summary(), "A match analysis summary is missing.", null);
            requireText(item.strengths(), "Match analysis strengths are missing.", null);
            requireText(item.gaps(), "Match analysis gaps are missing.", null);
            requireText(
                    item.recommendations(),
                    "Match analysis recommendations are missing.",
                    null
            );
            requireText(item.modelName(), "A match analysis model is missing.", 100);
            require(item.createdAt() != null, "A match analysis creation date is missing.");
        }

        AttentionSettingsItem settings = data.attentionSettings();
        require(settings != null, "Reminder settings are missing.");
        requireDays(settings.appliedDays());
        requireDays(settings.onlineAssessmentDays());
        requireDays(settings.interviewDays());
        require(settings.updatedAt() != null, "The reminder settings date is missing.");

        for (AttentionEventItem item : data.attentionEvents()) {
            require(jobIds.contains(item.jobSourceId()), "A reminder event refers to a missing job.");
            require(item.action() != null, "A reminder event action is missing.");
            require(
                    item.previousSnoozedUntil() != null || item.newSnoozedUntil() != null,
                    "A reminder event must contain a previous or new reminder date."
            );
            require(item.createdAt() != null, "A reminder event creation date is missing.");
        }

        Set<Long> preparedActivityIds = new HashSet<>();
        for (InterviewPreparationItem item : data.interviewPreparations()) {
            require(
                    activityIds.contains(item.activitySourceId()),
                    "Interview preparation refers to a missing activity."
            );
            require(
                    preparedActivityIds.add(item.activitySourceId()),
                    "The data export contains duplicate interview preparation."
            );
            require(
                    activityById.get(item.activitySourceId()).type() == JobActivityType.INTERVIEW,
                    "Interview preparation refers to a non-interview activity."
            );
            validatePreparationSection(
                    item.companyResearch(),
                    item.companyResearchDone(),
                    "Company research"
            );
            validatePreparationSection(
                    item.rolePriorities(),
                    item.rolePrioritiesDone(),
                    "Role priorities"
            );
            validatePreparationSection(item.starStories(), item.starStoriesDone(), "STAR stories");
            validatePreparationSection(
                    item.questionsToAsk(),
                    item.questionsToAskDone(),
                    "Questions to ask"
            );
            require(item.updatedAt() != null, "An interview preparation date is missing.");
        }
    }

    private static void requireLists(ArchiveData data) {
        require(data.jobs() != null, "The jobs section is missing.");
        require(data.resumes() != null, "The resumes section is missing.");
        require(data.jobActivities() != null, "The job activities section is missing.");
        require(data.matchAnalyses() != null, "The match analyses section is missing.");
        require(data.attentionEvents() != null, "The reminder history section is missing.");
        require(
                data.interviewPreparations() != null,
                "The interview preparation section is missing."
        );
        requireSectionSize("jobs", data.jobs());
        requireSectionSize("resumes", data.resumes());
        requireSectionSize("job activities", data.jobActivities());
        requireSectionSize("match analyses", data.matchAnalyses());
        requireSectionSize("reminder history", data.attentionEvents());
        requireSectionSize("interview preparations", data.interviewPreparations());
    }

    private static <T> Set<Long> validateSourceIds(
            String section,
            List<T> items,
            Function<T, Long> idGetter
    ) {
        Set<Long> ids = new HashSet<>();
        for (T item : items) {
            require(item != null, "The " + section + " section contains an empty record.");
            Long id = idGetter.apply(item);
            require(id != null && id > 0, "The " + section + " section contains an invalid id.");
            require(ids.add(id), "The " + section + " section contains a duplicate id.");
        }
        return ids;
    }

    private static void validatePreparationSection(String value, boolean done, String label) {
        requireLength(value, 5000, label + " notes are too long.");
        if (done) {
            require(value != null && !value.isBlank(), label + " is marked done without notes.");
        }
    }

    private static void requireDays(int days) {
        require(days >= 1 && days <= 90, "Reminder days must be between 1 and 90.");
    }

    private static <T> void requireSectionSize(String section, List<T> items) {
        require(
                items.size() <= MAX_SECTION_RECORDS,
                "The " + section + " section contains too many records."
        );
    }

    private static void requireText(
            String value,
            String message,
            Integer maximumLength
    ) {
        require(value != null && !value.isBlank(), message);
        if (maximumLength != null) {
            requireLength(value, maximumLength, message);
        }
    }

    private static void requireLength(String value, int maximumLength, String message) {
        require(value == null || value.length() <= maximumLength, message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new DataTransferException(message);
        }
    }

    private void ensureImportEnabled() {
        if (!importEnabled) {
            throw new DataTransferImportDisabledException();
        }
    }

    private static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DataTransferException("Choose a CareerPilot data export to import.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new DataTransferException("CareerPilot data exports must be 20 MB or smaller.");
        }
        String filename = safeFilename(file.getOriginalFilename());
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new DataTransferException("Choose a CareerPilot JSON data export.");
        }
        return filename;
    }

    private DataTransferCountsResponse existingCounts() {
        return new DataTransferCountsResponse(
                jobRepository.count(),
                resumeRepository.count(),
                jobActivityRepository.count(),
                matchAnalysisRepository.count(),
                attentionSettingsRepository.count(),
                attentionEventRepository.count(),
                interviewPreparationRepository.count()
        );
    }

    private static DataTransferCountsResponse counts(ArchiveData data) {
        return new DataTransferCountsResponse(
                data.jobs().size(),
                data.resumes().size(),
                data.jobActivities().size(),
                data.matchAnalyses().size(),
                1,
                data.attentionEvents().size(),
                data.interviewPreparations().size()
        );
    }

    private static JobItem toJobItem(Job job) {
        return new JobItem(
                job.getId(),
                job.getCompany(),
                job.getTitle(),
                job.getDescription(),
                job.getJobUrl(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getAttentionSnoozedUntil()
        );
    }

    private static ResumeItem toResumeItem(Resume resume) {
        return new ResumeItem(
                resume.getId(),
                resume.getName(),
                resume.getContent(),
                resume.getCreatedAt()
        );
    }

    private static JobActivityItem toJobActivityItem(JobActivity activity) {
        return new JobActivityItem(
                activity.getId(),
                activity.getJob().getId(),
                activity.getType(),
                activity.getTitle(),
                activity.getDetails(),
                activity.getContact(),
                activity.getOccurredAt(),
                activity.getCompletedAt(),
                activity.getCompletionNote(),
                activity.getCompletionPreviousJobStatus(),
                activity.getCompletionAppliedJobStatus(),
                activity.getCreatedAt()
        );
    }

    private static MatchAnalysisItem toMatchAnalysisItem(MatchAnalysis analysis) {
        return new MatchAnalysisItem(
                analysis.getId(),
                analysis.getJob().getId(),
                analysis.getResume().getId(),
                analysis.getMatchScore(),
                analysis.getSummary(),
                analysis.getStrengths(),
                analysis.getGaps(),
                analysis.getRecommendations(),
                analysis.getModelName(),
                analysis.getCreatedAt()
        );
    }

    private static AttentionSettingsItem toAttentionSettingsItem(JobAttentionSettings settings) {
        return new AttentionSettingsItem(
                settings.getAppliedDays(),
                settings.getOnlineAssessmentDays(),
                settings.getInterviewDays(),
                settings.getUpdatedAt()
        );
    }

    private static AttentionEventItem toAttentionEventItem(JobAttentionEvent event) {
        return new AttentionEventItem(
                event.getId(),
                event.getJob().getId(),
                event.getAction(),
                event.getPreviousSnoozedUntil(),
                event.getNewSnoozedUntil(),
                event.getCreatedAt()
        );
    }

    private static InterviewPreparationItem toInterviewPreparationItem(
            InterviewPreparation preparation
    ) {
        return new InterviewPreparationItem(
                preparation.getId(),
                preparation.getActivity().getId(),
                preparation.getCompanyResearch(),
                preparation.isCompanyResearchDone(),
                preparation.getRolePriorities(),
                preparation.isRolePrioritiesDone(),
                preparation.getStarStories(),
                preparation.isStarStoriesDone(),
                preparation.getQuestionsToAsk(),
                preparation.isQuestionsToAskDone(),
                preparation.getUpdatedAt()
        );
    }

    private static String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "careerpilot-data.json";
        }
        String normalized = originalFilename.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).trim();
    }

    private record ParsedArchive(String filename, CareerPilotArchive archive) {
    }
}
