package com.careerpilot.service;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobActivityCalendarService {

    private static final String CRLF = "\r\n";
    private static final int MAX_LINE_OCTETS = 75;
    private static final DateTimeFormatter UTC_DATE_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;
    private final Clock clock;

    public JobActivityCalendarService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public JobActivityCalendarFile export(Long jobId, Long activityId) {
        jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        JobActivity activity = jobActivityRepository.findByIdAndJob_Id(activityId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job activity", activityId));
        validateExportable(activity);

        String calendar = buildCalendar(List.of(activity), clock.instant());
        return new JobActivityCalendarFile(
                "careerpilot-activity-" + activityId + ".ics",
                calendar.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Transactional(readOnly = true)
    public JobActivityCalendarFile export(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            throw new JobActivityCalendarException("At least one activity is required.");
        }

        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(activityIds));
        Map<Long, JobActivity> activitiesById = jobActivityRepository.findAllByIdIn(uniqueIds)
                .stream()
                .collect(Collectors.toMap(JobActivity::getId, Function.identity()));
        for (Long activityId : uniqueIds) {
            if (!activitiesById.containsKey(activityId)) {
                throw new ResourceNotFoundException("Job activity", activityId);
            }
        }

        List<JobActivity> activities = uniqueIds.stream()
                .map(activitiesById::get)
                .sorted(Comparator.comparing(JobActivity::getOccurredAt)
                        .thenComparing(JobActivity::getId))
                .toList();
        activities.forEach(JobActivityCalendarService::validateExportable);

        Instant generatedAt = clock.instant();
        String calendar = buildCalendar(activities, generatedAt);
        return new JobActivityCalendarFile(
                "careerpilot-calendar-" + formatUtc(generatedAt) + ".ics",
                calendar.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String buildCalendar(List<JobActivity> activities, Instant generatedAt) {
        List<String> lines = new ArrayList<>(List.of(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//CareerPilot//Job Activity//EN",
                "CALSCALE:GREGORIAN"
        ));
        activities.forEach(activity -> {
            Job job = activity.getJob();
            lines.addAll(List.of(
                    "BEGIN:VEVENT",
                    "UID:job-" + job.getId() + "-activity-" + activity.getId()
                            + "@careerpilot.local",
                    "DTSTAMP:" + formatUtc(generatedAt),
                    "DTSTART:" + formatUtc(activity.getOccurredAt()),
                    "SUMMARY:" + escapeText(activity.getTitle() + " - " + job.getCompany()),
                    "DESCRIPTION:" + escapeText(description(job, activity)),
                    "STATUS:CONFIRMED",
                    "END:VEVENT"
            ));
        });
        lines.add("END:VCALENDAR");

        return lines.stream()
                .map(JobActivityCalendarService::foldLine)
                .reduce((left, right) -> left + CRLF + right)
                .orElseThrow()
                + CRLF;
    }

    private static void validateExportable(JobActivity activity) {
        if (activity.getType() != JobActivityType.INTERVIEW
                && activity.getType() != JobActivityType.FOLLOW_UP) {
            throw new JobActivityCalendarException(
                    "Only interviews and follow-ups can be exported to a calendar."
            );
        }
    }

    private static String description(Job job, JobActivity activity) {
        List<String> parts = new ArrayList<>();
        if (activity.getDetails() != null) {
            parts.add(activity.getDetails());
        }
        parts.add("Job: " + job.getTitle() + " at " + job.getCompany());
        if (activity.getContact() != null) {
            parts.add("Contact: " + activity.getContact());
        }
        return String.join("\n", parts);
    }

    private static String formatUtc(Instant instant) {
        return UTC_DATE_TIME.format(instant);
    }

    private static String escapeText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", "\\n")
                .replace(";", "\\;")
                .replace(",", "\\,");
    }

    private static String foldLine(String line) {
        StringBuilder folded = new StringBuilder();
        int currentOctets = 0;

        for (int offset = 0; offset < line.length(); ) {
            int codePoint = line.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterOctets = character.getBytes(StandardCharsets.UTF_8).length;

            if (currentOctets + characterOctets > MAX_LINE_OCTETS) {
                folded.append(CRLF).append(' ');
                currentOctets = 1;
            }

            folded.append(character);
            currentOctets += characterOctets;
            offset += Character.charCount(codePoint);
        }

        return folded.toString();
    }
}
