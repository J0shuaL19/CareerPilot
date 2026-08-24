package com.careerpilot.service;

import com.careerpilot.dto.InterviewPreparationRequest;
import com.careerpilot.dto.InterviewPreparationResponse;
import com.careerpilot.exception.InterviewPreparationException;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.InterviewPreparation;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.InterviewPreparationRepository;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewPreparationService {

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;
    private final InterviewPreparationRepository interviewPreparationRepository;
    private final Clock clock;

    public InterviewPreparationService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            InterviewPreparationRepository interviewPreparationRepository,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.interviewPreparationRepository = interviewPreparationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InterviewPreparationResponse getPreparation(Long jobId, Long activityId) {
        JobActivity activity = findInterview(jobId, activityId);
        return interviewPreparationRepository.findByActivity_Id(activityId)
                .map(InterviewPreparationService::toResponse)
                .orElseGet(() -> emptyResponse(activity.getId()));
    }

    @Transactional
    public InterviewPreparationResponse savePreparation(
            Long jobId,
            Long activityId,
            InterviewPreparationRequest request
    ) {
        JobActivity activity = findInterview(jobId, activityId);
        String companyResearch = normalizeOptional(request.companyResearch());
        String rolePriorities = normalizeOptional(request.rolePriorities());
        String starStories = normalizeOptional(request.starStories());
        String questionsToAsk = normalizeOptional(request.questionsToAsk());

        validateCompletedSection(
                request.companyResearchDone(),
                companyResearch,
                "Company research"
        );
        validateCompletedSection(
                request.rolePrioritiesDone(),
                rolePriorities,
                "Role priorities"
        );
        validateCompletedSection(
                request.starStoriesDone(),
                starStories,
                "STAR stories"
        );
        validateCompletedSection(
                request.questionsToAskDone(),
                questionsToAsk,
                "Questions to ask"
        );

        InterviewPreparation preparation = interviewPreparationRepository
                .findByActivity_Id(activityId)
                .orElseGet(() -> new InterviewPreparation(activity, clock.instant()));
        preparation.update(
                companyResearch,
                request.companyResearchDone(),
                rolePriorities,
                request.rolePrioritiesDone(),
                starStories,
                request.starStoriesDone(),
                questionsToAsk,
                request.questionsToAskDone(),
                clock.instant()
        );
        return toResponse(interviewPreparationRepository.save(preparation));
    }

    private JobActivity findInterview(Long jobId, Long activityId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job", jobId);
        }
        JobActivity activity = jobActivityRepository.findByIdAndJob_Id(activityId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job activity", activityId));
        if (activity.getType() != JobActivityType.INTERVIEW) {
            throw new InterviewPreparationException(
                    "Interview preparation is only available for interview activities."
            );
        }
        return activity;
    }

    private static void validateCompletedSection(boolean completed, String notes, String label) {
        if (completed && notes == null) {
            throw new InterviewPreparationException(
                    label + " needs notes before it can be marked complete."
            );
        }
    }

    private static InterviewPreparationResponse emptyResponse(Long activityId) {
        return new InterviewPreparationResponse(
                activityId,
                null,
                false,
                null,
                false,
                null,
                false,
                null,
                false,
                0,
                InterviewPreparation.TOTAL_SECTIONS,
                0,
                null
        );
    }

    private static InterviewPreparationResponse toResponse(InterviewPreparation preparation) {
        int completedSections = preparation.completedSections();
        return new InterviewPreparationResponse(
                preparation.getActivity().getId(),
                preparation.getCompanyResearch(),
                preparation.isCompanyResearchDone(),
                preparation.getRolePriorities(),
                preparation.isRolePrioritiesDone(),
                preparation.getStarStories(),
                preparation.isStarStoriesDone(),
                preparation.getQuestionsToAsk(),
                preparation.isQuestionsToAskDone(),
                completedSections,
                InterviewPreparation.TOTAL_SECTIONS,
                preparation.progressPercent(),
                preparation.getUpdatedAt()
        );
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
