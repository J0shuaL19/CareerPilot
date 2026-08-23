package com.careerpilot.service;

import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.repository.JobRepository;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobCsvExportService {

    private static final String FILENAME = "careerpilot-jobs.csv";
    private static final String HEADER = "ID,Company,Title,Status,Job URL,Created at,Description\r\n";

    private final JobRepository jobRepository;

    public JobCsvExportService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public JobCsvFile export(List<Long> requestedJobIds) {
        List<Long> jobIds = List.copyOf(new LinkedHashSet<>(requestedJobIds));
        Map<Long, Job> jobsById = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));

        List<Job> jobs = jobIds.stream()
                .map(id -> {
                    Job job = jobsById.get(id);
                    if (job == null) {
                        throw new ResourceNotFoundException("Job", id);
                    }
                    return job;
                })
                .toList();

        StringBuilder csv = new StringBuilder(HEADER);
        jobs.forEach(job -> appendRow(csv, job));

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[csvBytes.length + 3];
        content[0] = (byte) 0xEF;
        content[1] = (byte) 0xBB;
        content[2] = (byte) 0xBF;
        System.arraycopy(csvBytes, 0, content, 3, csvBytes.length);

        return new JobCsvFile(FILENAME, content);
    }

    private static void appendRow(StringBuilder csv, Job job) {
        csv.append(job.getId()).append(',')
                .append(csvValue(job.getCompany())).append(',')
                .append(csvValue(job.getTitle())).append(',')
                .append(job.getStatus()).append(',')
                .append(csvValue(job.getJobUrl())).append(',')
                .append(job.getCreatedAt()).append(',')
                .append(csvValue(job.getDescription()))
                .append("\r\n");
    }

    private static String csvValue(String value) {
        if (value == null) {
            return "\"\"";
        }

        String safeValue = neutralizeSpreadsheetFormula(value);
        return '"' + safeValue.replace("\"", "\"\"") + '"';
    }

    private static String neutralizeSpreadsheetFormula(String value) {
        int firstCharacter = 0;
        while (firstCharacter < value.length() && Character.isWhitespace(value.charAt(firstCharacter))) {
            firstCharacter++;
        }
        if (firstCharacter == value.length()) {
            return value;
        }

        return switch (value.charAt(firstCharacter)) {
            case '=', '+', '-', '@' -> "'" + value;
            default -> value;
        };
    }
}
