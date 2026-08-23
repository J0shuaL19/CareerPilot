package com.careerpilot.service;

import com.careerpilot.dto.DashboardStatsRange;
import com.careerpilot.dto.DashboardStatsResponse;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardStatsService {

    private static final List<JobActivityType> FUNNEL_ACTIVITY_TYPES = List.of(
            JobActivityType.APPLICATION,
            JobActivityType.INTERVIEW
    );

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;
    private final Clock clock;

    public DashboardStatsService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(DashboardStatsRange range) {
        Instant to = clock.instant();
        Instant from = rangeStart(range, to);
        List<Job> jobs = from == null
                ? jobRepository.findAllByOrderByCreatedAtDesc()
                : jobRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(from);

        if (jobs.isEmpty()) {
            return new DashboardStatsResponse(range, from, to, 0, 0, 0, 0, null, null, null);
        }

        Set<Long> applicationJobIds = new HashSet<>();
        Set<Long> interviewJobIds = new HashSet<>();
        Set<Long> offerJobIds = new HashSet<>();

        jobs.forEach(job -> addCurrentStage(job, applicationJobIds, interviewJobIds, offerJobIds));

        List<Long> jobIds = jobs.stream().map(Job::getId).toList();
        jobActivityRepository.findAllByJob_IdInAndTypeIn(jobIds, FUNNEL_ACTIVITY_TYPES)
                .forEach(activity -> addActivityStage(activity, applicationJobIds, interviewJobIds));

        int trackedJobs = jobs.size();
        int applications = applicationJobIds.size();
        int interviews = interviewJobIds.size();
        int offers = offerJobIds.size();

        return new DashboardStatsResponse(
                range,
                from,
                to,
                trackedJobs,
                applications,
                interviews,
                offers,
                percentage(applications, trackedJobs),
                percentage(interviews, applications),
                percentage(offers, interviews)
        );
    }

    private static Instant rangeStart(DashboardStatsRange range, Instant now) {
        return switch (range) {
            case LAST_30_DAYS -> now.minus(30, ChronoUnit.DAYS);
            case LAST_90_DAYS -> now.minus(90, ChronoUnit.DAYS);
            case ALL_TIME -> null;
        };
    }

    private static void addCurrentStage(
            Job job,
            Set<Long> applicationJobIds,
            Set<Long> interviewJobIds,
            Set<Long> offerJobIds
    ) {
        Long jobId = job.getId();
        if (job.getStatus() != JobStatus.SAVED) {
            applicationJobIds.add(jobId);
        }
        if (job.getStatus() == JobStatus.INTERVIEW || job.getStatus() == JobStatus.OFFER) {
            interviewJobIds.add(jobId);
        }
        if (job.getStatus() == JobStatus.OFFER) {
            offerJobIds.add(jobId);
        }
    }

    private static void addActivityStage(
            JobActivity activity,
            Set<Long> applicationJobIds,
            Set<Long> interviewJobIds
    ) {
        Long jobId = activity.getJob().getId();
        applicationJobIds.add(jobId);
        if (activity.getType() == JobActivityType.INTERVIEW) {
            interviewJobIds.add(jobId);
        }
    }

    private static Integer percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return null;
        }
        return (int) Math.round(numerator * 100.0 / denominator);
    }
}
