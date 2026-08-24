package com.careerpilot.service;

import com.careerpilot.dto.JobActivityCalendarImportEventResponse;
import com.careerpilot.dto.JobActivityCalendarImportItemRequest;
import com.careerpilot.dto.JobActivityCalendarImportPreviewResponse;
import com.careerpilot.dto.JobActivityCalendarImportResultResponse;
import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.exception.JobActivityCalendarImportException;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.JobActivityRepository;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
public class JobActivityCalendarImportService {

    static final long MAX_FILE_SIZE_BYTES = 1024L * 1024;
    static final int MAX_EVENTS = 100;

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME_SECONDS =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");

    private final JobActivityRepository jobActivityRepository;
    private final JobActivityService jobActivityService;
    private final Clock clock;

    public JobActivityCalendarImportService(
            JobActivityRepository jobActivityRepository,
            JobActivityService jobActivityService,
            Clock clock
    ) {
        this.jobActivityRepository = jobActivityRepository;
        this.jobActivityService = jobActivityService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public JobActivityCalendarImportPreviewResponse preview(MultipartFile file) {
        String filename = validateFile(file);
        List<List<String>> rawEvents = extractEvents(readUtf8(file));
        if (rawEvents.isEmpty()) {
            throw new JobActivityCalendarImportException(
                    "The calendar file does not contain any events."
            );
        }
        if (rawEvents.size() > MAX_EVENTS) {
            throw new JobActivityCalendarImportException(
                    "Calendar files can contain at most 100 events."
            );
        }

        List<JobActivityCalendarImportEventResponse> events = new ArrayList<>();
        for (int index = 0; index < rawEvents.size(); index++) {
            events.add(parseEvent(index + 1, rawEvents.get(index)));
        }
        int importable = (int) events.stream()
                .filter(JobActivityCalendarImportEventResponse::importable)
                .count();
        return new JobActivityCalendarImportPreviewResponse(
                filename,
                events.size(),
                importable,
                events.size() - importable,
                List.copyOf(events)
        );
    }

    @Transactional
    public JobActivityCalendarImportResultResponse importEvents(
            List<JobActivityCalendarImportItemRequest> events
    ) {
        Set<String> requestKeys = new HashSet<>();
        int imported = 0;
        int skippedDuplicates = 0;

        for (JobActivityCalendarImportItemRequest event : events) {
            if (event.type() != JobActivityType.INTERVIEW
                    && event.type() != JobActivityType.FOLLOW_UP) {
                throw new JobActivityCalendarImportException(
                        "Calendar imports only support interviews and follow-ups."
                );
            }

            String title = event.title().trim();
            String duplicateKey = event.jobId() + "\u0000" + event.type() + "\u0000"
                    + title.toLowerCase(Locale.ROOT) + "\u0000" + event.occurredAt();
            boolean duplicateInRequest = !requestKeys.add(duplicateKey);
            boolean duplicateInDatabase = jobActivityRepository
                    .existsByJob_IdAndTypeAndTitleIgnoreCaseAndOccurredAt(
                            event.jobId(), event.type(), title, event.occurredAt()
                    );
            if (duplicateInRequest || duplicateInDatabase) {
                skippedDuplicates++;
                continue;
            }

            jobActivityService.createActivity(event.jobId(), new JobActivityRequest(
                    event.type(),
                    title,
                    normalizeOptional(event.details()),
                    normalizeOptional(event.contact()),
                    event.occurredAt()
            ));
            imported++;
        }
        return new JobActivityCalendarImportResultResponse(imported, skippedDuplicates);
    }

    private JobActivityCalendarImportEventResponse parseEvent(int eventNumber, List<String> lines) {
        Map<String, CalendarProperty> properties = new LinkedHashMap<>();
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String declaration = line.substring(0, separator);
            String name = declaration.split(";", 2)[0].toUpperCase(Locale.ROOT);
            properties.putIfAbsent(
                    name,
                    new CalendarProperty(declaration, line.substring(separator + 1))
            );
        }

        List<String> errors = new ArrayList<>();
        String title = unescapeText(value(properties, "SUMMARY")).trim();
        String details = normalizeOptional(unescapeText(value(properties, "DESCRIPTION")));
        String contact = normalizeOptional(unescapeText(value(properties, "LOCATION")));
        Instant occurredAt = parseStart(properties.get("DTSTART"), errors);

        if (title.isBlank()) {
            errors.add("Event title is required");
            title = "Untitled calendar event";
        } else if (title.length() > 255) {
            errors.add("Event title must be 255 characters or fewer");
        }
        if (details != null && details.length() > 5000) {
            errors.add("Event description must be 5000 characters or fewer");
        }
        if (contact != null && contact.length() > 255) {
            errors.add("Event location must be 255 characters or fewer");
        }

        return new JobActivityCalendarImportEventResponse(
                eventNumber,
                title,
                details,
                contact,
                occurredAt,
                suggestType(title, details),
                errors.isEmpty(),
                List.copyOf(errors)
        );
    }

    private Instant parseStart(CalendarProperty property, List<String> errors) {
        if (property == null || property.value().isBlank()) {
            errors.add("Event start time is required");
            return null;
        }

        String declaration = property.declaration();
        String value = property.value().trim();
        try {
            if (declaration.toUpperCase(Locale.ROOT).contains("VALUE=DATE")) {
                return LocalDate.parse(value, DATE)
                        .atStartOfDay(clock.getZone())
                        .toInstant();
            }
            if (value.endsWith("Z")) {
                return parseLocalDateTime(value.substring(0, value.length() - 1))
                        .toInstant(ZoneOffset.UTC);
            }
            String timezone = parameter(declaration, "TZID");
            ZoneId zone = timezone == null ? clock.getZone() : ZoneId.of(timezone);
            return parseLocalDateTime(value).atZone(zone).toInstant();
        } catch (DateTimeException exception) {
            errors.add("Event start time or timezone is not supported");
            return null;
        }
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        DateTimeFormatter formatter = value.length() == 13
                ? DATE_TIME_MINUTES
                : DATE_TIME_SECONDS;
        return LocalDateTime.parse(value, formatter);
    }

    private static String parameter(String declaration, String parameterName) {
        String prefix = parameterName.toUpperCase(Locale.ROOT) + "=";
        for (String part : declaration.split(";")) {
            if (part.toUpperCase(Locale.ROOT).startsWith(prefix)) {
                return part.substring(prefix.length()).replace("\"", "");
            }
        }
        return null;
    }

    private static JobActivityType suggestType(String title, String details) {
        String searchable = (title + " " + (details == null ? "" : details))
                .toLowerCase(Locale.ROOT);
        return searchable.contains("follow-up")
                || searchable.contains("follow up")
                || searchable.contains("followup")
                ? JobActivityType.FOLLOW_UP
                : JobActivityType.INTERVIEW;
    }

    private static List<List<String>> extractEvents(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String upperContent = normalized.toUpperCase(Locale.ROOT);
        if (!upperContent.contains("BEGIN:VCALENDAR")
                || !upperContent.contains("END:VCALENDAR")) {
            throw new JobActivityCalendarImportException(
                    "The file is not a valid iCalendar file."
            );
        }

        List<String> unfolded = new ArrayList<>();
        for (String line : normalized.split("\n", -1)) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && !unfolded.isEmpty()) {
                int last = unfolded.size() - 1;
                unfolded.set(last, unfolded.get(last) + line.substring(1));
            } else {
                unfolded.add(line);
            }
        }

        List<List<String>> events = new ArrayList<>();
        List<String> current = null;
        for (String line : unfolded) {
            if (line.equalsIgnoreCase("BEGIN:VEVENT")) {
                if (current != null) {
                    throw new JobActivityCalendarImportException(
                            "The calendar contains nested events."
                    );
                }
                current = new ArrayList<>();
            } else if (line.equalsIgnoreCase("END:VEVENT")) {
                if (current == null) {
                    throw new JobActivityCalendarImportException(
                            "The calendar contains an unmatched event end."
                    );
                }
                events.add(List.copyOf(current));
                current = null;
            } else if (current != null) {
                current.add(line);
            }
        }
        if (current != null) {
            throw new JobActivityCalendarImportException(
                    "The calendar contains an unclosed event."
            );
        }
        return events;
    }

    private static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JobActivityCalendarImportException(
                    "Choose an ICS file to import."
            );
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new JobActivityCalendarImportException(
                    "Calendar files must be 1 MB or smaller."
            );
        }

        String filename = safeFilename(file.getOriginalFilename());
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".ics")) {
            throw new JobActivityCalendarImportException(
                    "Only ICS calendar files are supported."
            );
        }
        return filename;
    }

    private static String readUtf8(MultipartFile file) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString()
                    .replace("\uFEFF", "");
        } catch (CharacterCodingException exception) {
            throw new JobActivityCalendarImportException(
                    "The calendar file must use UTF-8 encoding."
            );
        } catch (IOException exception) {
            throw new JobActivityCalendarImportException(
                    "We couldn't read this calendar file."
            );
        }
    }

    private static String value(Map<String, CalendarProperty> properties, String name) {
        CalendarProperty property = properties.get(name);
        return property == null ? "" : property.value();
    }

    private static String unescapeText(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\' && index + 1 < value.length()) {
                char escaped = value.charAt(++index);
                result.append(switch (escaped) {
                    case 'n', 'N' -> '\n';
                    case ',', ';', '\\' -> escaped;
                    default -> escaped;
                });
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "calendar.ics";
        }
        String normalized = originalFilename.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).trim();
    }

    private record CalendarProperty(String declaration, String value) {
    }
}