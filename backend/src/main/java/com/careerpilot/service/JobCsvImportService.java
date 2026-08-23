package com.careerpilot.service;

import com.careerpilot.dto.JobCsvImportPreviewResponse;
import com.careerpilot.dto.JobCsvImportResultResponse;
import com.careerpilot.dto.JobCsvImportRowResponse;
import com.careerpilot.dto.JobCsvImportRowState;
import com.careerpilot.dto.JobRequest;
import com.careerpilot.exception.JobCsvImportException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JobCsvImportService {

    static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;
    static final int MAX_DATA_ROWS = 1_000;

    private static final List<String> REQUIRED_HEADERS = List.of("company", "title", "description");

    private final JobRepository jobRepository;
    private final Validator validator;

    public JobCsvImportService(JobRepository jobRepository, Validator validator) {
        this.jobRepository = jobRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public JobCsvImportPreviewResponse preview(MultipartFile file) {
        EvaluatedFile evaluatedFile = evaluate(file);
        return toPreview(evaluatedFile);
    }

    @Transactional
    public JobCsvImportResultResponse importFile(MultipartFile file) {
        EvaluatedFile evaluatedFile = evaluate(file);
        List<Job> jobs = evaluatedFile.rows().stream()
                .filter(row -> row.state() == JobCsvImportRowState.VALID)
                .map(JobCsvImportService::toJob)
                .toList();
        jobRepository.saveAll(jobs);

        return new JobCsvImportResultResponse(
                jobs.size(),
                countState(evaluatedFile.rows(), JobCsvImportRowState.DUPLICATE),
                countState(evaluatedFile.rows(), JobCsvImportRowState.INVALID)
        );
    }

    private EvaluatedFile evaluate(MultipartFile file) {
        String filename = validateFile(file);
        List<List<String>> records = parseCsv(readUtf8(file));
        if (records.isEmpty()) {
            throw new JobCsvImportException("The CSV file is empty.");
        }

        Map<String, Integer> headers = parseHeaders(records.getFirst());
        List<List<String>> dataRecords = records.subList(1, records.size());
        if (dataRecords.isEmpty()) {
            throw new JobCsvImportException("The CSV file does not contain any job rows.");
        }
        if (dataRecords.size() > MAX_DATA_ROWS) {
            throw new JobCsvImportException("CSV files can contain at most 1000 job rows.");
        }

        Set<String> fileDuplicateKeys = new HashSet<>();
        List<JobCsvImportRowResponse> rows = new ArrayList<>();
        for (int index = 0; index < dataRecords.size(); index++) {
            rows.add(evaluateRow(index + 2, dataRecords.get(index), headers, fileDuplicateKeys));
        }
        return new EvaluatedFile(filename, rows);
    }

    private JobCsvImportRowResponse evaluateRow(
            int rowNumber,
            List<String> cells,
            Map<String, Integer> headers,
            Set<String> fileDuplicateKeys
    ) {
        String company = cell(cells, headers.get("company")).trim();
        String title = cell(cells, headers.get("title")).trim();
        String description = cell(cells, headers.get("description")).trim();
        String jobUrl = normalizeOptional(cell(cells, headers.get("joburl")));
        String statusValue = cell(cells, headers.get("status")).trim();
        JobStatus status = parseStatus(statusValue);

        List<String> errors = new ArrayList<>();
        JobRequest request = new JobRequest(company, title, description, jobUrl);
        validator.validate(request).stream()
                .sorted((left, right) -> left.getPropertyPath().toString()
                        .compareTo(right.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .forEach(errors::add);
        if (!statusValue.isBlank() && status == null) {
            errors.add("Status must be SAVED, APPLIED, OA, INTERVIEW, OFFER, REJECTED, or WITHDRAWN");
        }
        if (cells.size() > headers.size()
                && cells.subList(headers.size(), cells.size()).stream().anyMatch(value -> !value.isBlank())) {
            errors.add("Row contains more values than the header");
        }

        JobCsvImportRowState state = JobCsvImportRowState.INVALID;
        if (errors.isEmpty()) {
            String duplicateKey = duplicateKey(company, title);
            boolean duplicateInFile = !fileDuplicateKeys.add(duplicateKey);
            boolean duplicateInDatabase = jobRepository
                    .existsByCompanyIgnoreCaseAndTitleIgnoreCase(company, title);
            state = duplicateInFile || duplicateInDatabase
                    ? JobCsvImportRowState.DUPLICATE
                    : JobCsvImportRowState.VALID;
        }

        return new JobCsvImportRowResponse(
                rowNumber,
                company,
                title,
                description,
                jobUrl,
                status == null ? JobStatus.SAVED : status,
                state,
                List.copyOf(errors)
        );
    }

    private static Job toJob(JobCsvImportRowResponse row) {
        Job job = new Job(row.company(), row.title(), row.description(), row.jobUrl());
        if (row.status() != JobStatus.SAVED) {
            job.updateStatus(row.status());
        }
        return job;
    }

    private static JobCsvImportPreviewResponse toPreview(EvaluatedFile file) {
        return new JobCsvImportPreviewResponse(
                file.filename(),
                file.rows().size(),
                countState(file.rows(), JobCsvImportRowState.VALID),
                countState(file.rows(), JobCsvImportRowState.DUPLICATE),
                countState(file.rows(), JobCsvImportRowState.INVALID),
                file.rows()
        );
    }

    private static int countState(List<JobCsvImportRowResponse> rows, JobCsvImportRowState state) {
        return (int) rows.stream().filter(row -> row.state() == state).count();
    }

    private static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JobCsvImportException("Choose a CSV file to import.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new JobCsvImportException("Job CSV files must be 2 MB or smaller.");
        }

        String filename = safeFilename(file.getOriginalFilename());
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new JobCsvImportException("Only CSV job files are supported.");
        }
        return filename;
    }

    private static String readUtf8(MultipartFile file) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new JobCsvImportException("The CSV file must use UTF-8 encoding.");
        } catch (IOException exception) {
            throw new JobCsvImportException("We couldn't read this CSV file.");
        }
    }

    private static List<List<String>> parseCsv(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quotedFieldClosed = false;

        for (int index = 0; index < content.length(); index++) {
            char character = content.charAt(index);
            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                        quotedFieldClosed = true;
                    }
                } else {
                    field.append(character);
                }
                continue;
            }

            if (quotedFieldClosed && character != ',' && character != '\r' && character != '\n') {
                if (!Character.isWhitespace(character)) {
                    throw new JobCsvImportException(
                            "The CSV file contains text after a closed quoted value."
                    );
                }
            } else if (character == '"' && field.isEmpty()) {
                inQuotes = true;
            } else if (character == '"') {
                throw new JobCsvImportException("The CSV file contains an unexpected quote.");
            } else if (character == ',') {
                record.add(field.toString());
                field.setLength(0);
                quotedFieldClosed = false;
            } else if (character == '\r' || character == '\n') {
                record.add(field.toString());
                field.setLength(0);
                quotedFieldClosed = false;
                addRecordIfNotBlank(records, record);
                record = new ArrayList<>();
                if (character == '\r'
                        && index + 1 < content.length()
                        && content.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                field.append(character);
            }
        }

        if (inQuotes) {
            throw new JobCsvImportException("The CSV file contains an unclosed quoted value.");
        }
        record.add(field.toString());
        addRecordIfNotBlank(records, record);
        return records;
    }

    private static void addRecordIfNotBlank(List<List<String>> records, List<String> record) {
        if (record.stream().anyMatch(value -> !value.isBlank())) {
            records.add(List.copyOf(record));
        }
    }

    private static Map<String, Integer> parseHeaders(List<String> headerCells) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int index = 0; index < headerCells.size(); index++) {
            String header = normalizeHeader(headerCells.get(index));
            if (!header.isBlank() && headers.putIfAbsent(header, index) != null) {
                throw new JobCsvImportException("The CSV file contains a duplicate column: " + headerCells.get(index));
            }
        }
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!headers.containsKey(requiredHeader)) {
                throw new JobCsvImportException(
                        "The CSV file must include Company, Title, and Description columns."
                );
            }
        }
        return headers;
    }

    private static String normalizeHeader(String value) {
        return value.replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private static JobStatus parseStatus(String value) {
        if (value.isBlank()) {
            return JobStatus.SAVED;
        }
        try {
            return JobStatus.valueOf(value.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String cell(List<String> cells, Integer index) {
        return index == null || index >= cells.size() ? "" : cells.get(index);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String duplicateKey(String company, String title) {
        return company.toLowerCase(Locale.ROOT) + '\u0000' + title.toLowerCase(Locale.ROOT);
    }

    private static String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "jobs.csv";
        }
        String normalized = originalFilename.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).trim();
    }

    private record EvaluatedFile(String filename, List<JobCsvImportRowResponse> rows) {
    }
}
