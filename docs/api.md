# API design

Base URL during local development: `http://localhost:8080`

Request and response bodies use JSON unless an endpoint specifies otherwise. Dates use ISO 8601 UTC timestamps.

## Foundation endpoint

### `GET /api/health`

Confirms that the backend is running.

```json
{
  "status": "ok",
  "service": "careerpilot-backend"
}
```

## Dashboard statistics

### Get application funnel statistics

`GET /api/dashboard/stats?range=LAST_90_DAYS`

`range` is optional and defaults to `LAST_90_DAYS`. Supported values are `LAST_30_DAYS`, `LAST_90_DAYS`, and `ALL_TIME`.

The selected range forms a cohort from each job's `createdAt` time. Application and interview activities preserve reached stages even after a job moves to a closed status.

Success: `200 OK`

```json
{
  "range": "LAST_90_DAYS",
  "from": "2026-05-25T12:00:00Z",
  "to": "2026-08-23T12:00:00Z",
  "trackedJobs": 10,
  "applications": 8,
  "interviews": 4,
  "offers": 1,
  "applicationRate": 80,
  "interviewRate": 50,
  "offerRate": 25
}
```

`applicationRate` is applications divided by tracked jobs, `interviewRate` is interviews divided by applications, and `offerRate` is offers divided by interviews. A rate is `null` when its denominator is zero.

## Jobs

### Create a job

`POST /api/jobs`

Request:

```json
{
  "company": "OpenAI",
  "title": "Software Engineer",
  "description": "Build reliable products.",
  "jobUrl": "https://example.com/jobs/1"
}
```

Validation rules:

- `company`, `title`, and `description` are required.
- `company` and `title` can contain at most 255 characters.
- `jobUrl` is optional, must be a valid URL, and can contain at most 2048 characters.

Success: `201 Created`

```json
{
  "id": 1,
  "company": "OpenAI",
  "title": "Software Engineer",
  "description": "Build reliable products.",
  "jobUrl": "https://example.com/jobs/1",
  "status": "SAVED",
  "createdAt": "2026-08-19T02:00:00Z",
  "attentionSnoozedUntil": null
}
```

### List jobs

`GET /api/jobs`

Returns jobs ordered from newest to oldest.

Success: `200 OK`

```json
[
  {
    "id": 1,
    "company": "OpenAI",
    "title": "Software Engineer",
    "description": "Build reliable products.",
    "jobUrl": "https://example.com/jobs/1",
    "status": "SAVED",
    "createdAt": "2026-08-19T02:00:00Z",
    "attentionSnoozedUntil": null
  }
]
```

An empty database returns `[]`.

### Preview a CSV job import

`POST /api/jobs/import/preview`

Send a UTF-8 CSV file in the `file` field of a `multipart/form-data` request. Files can be at most 2 MB and contain at most 1,000 job rows.

`Company`, `Title`, and `Description` columns are required. `Job URL` and `Status` are optional; a blank status defaults to `SAVED`. Extra columns from CareerPilot exports, including `ID` and `Created at`, are ignored.

Success: `200 OK`

```json
{
  "filename": "careerpilot-jobs.csv",
  "totalRows": 3,
  "validRows": 1,
  "duplicateRows": 1,
  "invalidRows": 1,
  "rows": [
    {
      "rowNumber": 2,
      "company": "OpenAI",
      "title": "Software Engineer",
      "description": "Build reliable products.",
      "jobUrl": "https://example.com/jobs/1",
      "status": "APPLIED",
      "state": "VALID",
      "errors": []
    }
  ]
}
```

Row states are `VALID`, `DUPLICATE`, and `INVALID`. Duplicate matching uses company and title without case sensitivity and checks both saved jobs and earlier rows in the same file. Previewing never writes to the database.

### Import jobs from CSV

`POST /api/jobs/import`

Send the same file format as the preview endpoint. The backend parses and validates the file again, imports only rows currently marked valid, and skips duplicate or invalid rows.

Success: `200 OK`

```json
{
  "imported": 1,
  "skippedDuplicates": 1,
  "skippedInvalid": 1
}
```

### Export jobs as CSV

`POST /api/jobs/export`

Request:

```json
{
  "jobIds": [2, 1]
}
```

Downloads the selected jobs in the same order as `jobIds`. The frontend sends the IDs from the current filtered and sorted view, so users can export either the complete pipeline or only the opportunities they are reviewing.

Success: `200 OK` with a UTF-8 `text/csv` attachment named `careerpilot-jobs.csv`. The file includes ID, company, title, status, job URL, creation time, and description columns. Text is CSV-escaped, and potentially executable spreadsheet formula values are neutralized.

At least one positive job ID is required. A missing job returns `404 Not Found` so a stale list cannot produce a partial export.

### Get one job

`GET /api/jobs/{id}`

Success: `200 OK` with one job response.

Missing job: `404 Not Found`

```json
{
  "timestamp": "2026-08-19T02:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Job not found with id: 999",
  "path": "/api/jobs/999",
  "fieldErrors": {}
}
```

### Update job status

`PATCH /api/jobs/{id}/status`

Request:

```json
{
  "status": "INTERVIEW"
}
```

Success: `200 OK` with the complete updated job response. The change is persisted immediately.

`status` is required and must be one of the values listed below. A missing status returns `400 Bad Request`; a missing job returns `404 Not Found`.

### Update job details

`PUT /api/jobs/{id}`

Uses the same request body and validation rules as job creation. Success: `200 OK` with the complete updated job response. The job status and creation time remain unchanged.

### Snooze a job reminder

`PUT /api/jobs/{id}/attention-snooze`

Temporarily hides one overdue job from the daily action center without changing the global stage rules.

```json
{
  "snoozedUntil": "2026-08-30"
}
```

The date is required, uses `YYYY-MM-DD`, and must be in the future. Success: `200 OK` with the complete updated job response. The reminder becomes eligible again on the selected date if the job is still overdue. Creating a new activity for the job clears its snooze automatically.

### Clear a job reminder snooze

`DELETE /api/jobs/{id}/attention-snooze`

Clears the saved date immediately. Success: `200 OK` with the complete updated job response and `attentionSnoozedUntil` set to `null`.

The dashboard derives its `Snoozed reminders` management list from active jobs returned by `GET /api/jobs` whose `attentionSnoozedUntil` date is still in the future. Users can update that date through the snooze endpoint or resume the reminder immediately through the clear endpoint.

### Restore one job reminder state

`PUT /api/jobs/{id}/attention-snooze/restore`

Used by the dashboard's single-item Undo action. Pass the previous future date to undo a date change, or `null` to undo an initial pause.

```json
{
  "snoozedUntil": "2026-08-30"
}
```

Success: `200 OK` with the complete updated job response. The restored change is recorded in reminder history as `RESTORED`.

### Snooze multiple job reminders

`PUT /api/jobs/attention-snooze`

Applies one future return date to every selected job in a single transaction.

```json
{
  "jobIds": [1, 2],
  "snoozedUntil": "2026-09-01"
}
```

### Clear multiple job reminder snoozes

`POST /api/jobs/attention-snooze/clear`

```json
{
  "jobIds": [1, 2]
}
```

### Restore multiple job reminder dates

`PUT /api/jobs/attention-snooze/restore`

Restores each selected reminder to its own previous date. The dashboard uses this endpoint for one-click Undo after a bulk date change or bulk resume.

```json
{
  "reminders": [
    { "jobId": 1, "snoozedUntil": "2026-08-30" },
    { "jobId": 2, "snoozedUntil": "2026-09-02" }
  ]
}
```

All three bulk endpoints process between 1 and 100 reminders and return complete job responses in request order. Job IDs must be positive, and every restore date must be in the future. If any selected job is missing, the complete operation fails with `404 Not Found` and no reminder is changed.

### Delete a job

`DELETE /api/jobs/{id}`

Success: `204 No Content`. Deleting a job also deletes its saved match analyses. A missing job returns `404 Not Found`.

### Create a job activity

`POST /api/jobs/{jobId}/activities`

Request:

```json
{
  "type": "INTERVIEW",
  "title": "Technical interview",
  "details": "System design and collaboration round.",
  "contact": "Alex Chen, recruiter",
  "occurredAt": "2026-08-25T18:00:00Z"
}
```

`type`, `title`, and `occurredAt` are required. `title` and `contact` can contain at most 255 characters, and `details` can contain at most 5000 characters.

Success: `201 Created` with the saved activity. Activity types are `APPLICATION`, `INTERVIEW`, `FOLLOW_UP`, and `NOTE`.

### List job activities

`GET /api/jobs/{jobId}/activities`

Returns the job's activities ordered by occurrence time from newest to oldest. Completed activities remain in the list with `completedAt` and optional `completionNote`. Success: `200 OK`; a job without activity returns `[]`. A missing job returns `404 Not Found`.

### Update a job activity

`PUT /api/jobs/{jobId}/activities/{activityId}`

Uses the same request body and validation rules as activity creation. Success: `200 OK` with the complete updated activity. The activity must belong to the job in the request path; missing jobs or activities return `404 Not Found`.

### Complete a scheduled activity

`PUT /api/jobs/{jobId}/activities/{activityId}/complete`

Marks an interview or follow-up complete, records an optional outcome, and can update the owning job's pipeline stage in the same transaction.

```json
{
  "note": "Strong conversation; send architecture examples by Friday.",
  "jobStatus": "INTERVIEW"
}
```

`note` is optional and can contain at most 2000 characters. `jobStatus` is optional; omitting it preserves the current job stage. Accepted stages are `SAVED`, `APPLIED`, `OA`, `INTERVIEW`, `OFFER`, `REJECTED`, and `WITHDRAWN`.

Success: `200 OK` with the completed activity, including `completedAt` and `completionNote`. Completing an application or general note, or completing the same activity twice, returns `400 Bad Request`. Missing jobs or activities return `404 Not Found`.

### Reopen a completed activity

`PUT /api/jobs/{jobId}/activities/{activityId}/reopen`

Clears `completedAt` and `completionNote` so an interview or follow-up can be actioned again. The response contains the reopened `activity`, the current `jobStatus`, and `jobStatusRestored`.

If completion changed the job stage and that stage has not changed since, reopening restores the stage that was active before completion and returns `jobStatusRestored: true`. If the stage was changed again afterward, reopening preserves the newer stage and returns `jobStatusRestored: false`.

Success: `200 OK`. Reopening an activity that is not completed returns `400 Bad Request`; missing jobs or activities return `404 Not Found`. A future reopened activity returns to the upcoming action list, while a past activity returns to the overdue action list.

### Reschedule an activity

`PUT /api/jobs/{jobId}/activities/{activityId}/reschedule`

Moves an incomplete interview or follow-up to a new future time without creating a duplicate timeline entry.

```json
{
  "occurredAt": "2026-08-27T18:00:00Z"
}
```

`occurredAt` is required and must be later than the current server time. A successful reschedule also clears any saved attention snooze for the job and records that change in reminder history. Success: `200 OK` with the updated activity. Applications, general notes, completed activities, and past or current replacement times return `400 Bad Request`. Missing jobs or activities return `404 Not Found`.

### Export a job activity to a calendar

`GET /api/jobs/{jobId}/activities/{activityId}/calendar`

Downloads an RFC 5545 iCalendar file for an interview or follow-up. Success: `200 OK` with a UTF-8 `text/calendar` attachment. Applications and general notes return `400 Bad Request`; missing jobs or activities return `404 Not Found`.

### Get interview preparation

`GET /api/jobs/{jobId}/activities/{activityId}/preparation`

Returns the four-part preparation checklist attached to an interview activity. Before the first save, all note fields are `null`, all completion fields are `false`, `completedSections` and `progressPercent` are zero, and `updatedAt` is `null`.

Only `INTERVIEW` activities support preparation. Follow-ups, applications, and notes return `400 Bad Request`; a missing job or activity returns `404 Not Found`.

### Save interview preparation

`PUT /api/jobs/{jobId}/activities/{activityId}/preparation`

```json
{
  "companyResearch": "Product, market, and recent company news",
  "companyResearchDone": true,
  "rolePriorities": "Top outcomes and matching proof points",
  "rolePrioritiesDone": true,
  "starStories": "Impact, ambiguity, and collaboration examples",
  "starStoriesDone": false,
  "questionsToAsk": "What should this person achieve in 90 days?",
  "questionsToAskDone": false
}
```

Each note field is optional and limited to 5000 characters. A section can only be marked complete when its note field contains non-blank text. Values are trimmed before storage. The response returns the saved fields plus `completedSections`, `totalSections`, `progressPercent`, and `updatedAt`. Repeated saves update the same checklist rather than creating additional records.

### Delete a job activity

`DELETE /api/jobs/{jobId}/activities/{activityId}`

Success: `204 No Content`. The activity must belong to the job in the request path. Missing jobs or activities return `404 Not Found`.

### List overdue job activities

`GET /api/job-activities/overdue`

Returns incomplete interviews and follow-ups whose scheduled time is before the current server time, ordered from oldest to newest. These activities appear first in the daily action center and can be completed, rescheduled, or opened from their owning job. Success: `200 OK`; when nothing is overdue, the endpoint returns `[]`.

### List upcoming job activities

`GET /api/job-activities/upcoming`

Returns incomplete interviews and follow-ups scheduled from the current server time through the next 14 days, ordered from nearest to latest. Completed or past activities, applications, and general notes are excluded.

Each response includes `jobId`, `company`, and `jobTitle` so the frontend can present reminders across the complete pipeline. Success: `200 OK`; no upcoming reminders returns `[]`.

### List scheduled activities for a calendar range

`GET /api/job-activities/calendar?start={instant}&end={instant}`

Returns interviews and follow-ups whose scheduled time is greater than or equal to `start` and strictly before `end`, ordered chronologically. Both incomplete and completed activities are included; each item includes `jobId`, `company`, `jobTitle`, and nullable `completedAt` so month and agenda views can distinguish active work from completed history.

Interview items also include `preparationCompletedSections`, `preparationTotalSections`, and `preparationProgressPercent`. An interview without a saved checklist returns `0`, `4`, and `0`; follow-up items return `null` for all three fields. Preparations for the requested activities are loaded in one batch rather than one query per interview.

Both query parameters use ISO-8601 instants. `end` must be later than `start`, and the requested range cannot exceed 62 days. Invalid ranges return `400 Bad Request`; an empty range returns `[]` with `200 OK`.

### Export selected calendar activities

`POST /api/job-activities/calendar/export`

Downloads one UTF-8 RFC 5545 iCalendar file containing the selected interviews and follow-ups as chronologically ordered `VEVENT` entries.

```json
{
  "activityIds": [12, 18, 24]
}
```

The request accepts between 1 and 500 positive activity IDs. Duplicate IDs are exported once. Every selected activity must exist and be an interview or follow-up; a missing activity returns `404 Not Found` without producing a partial file, while an unsupported activity type returns `400 Bad Request`. Success: `200 OK` with a `text/calendar;charset=UTF-8` attachment. The Calendar page sends the IDs from its current month or week after applying search, type, completion status, and job filters.

### Preview an ICS calendar import

`POST /api/job-activities/calendar/import/preview`

Send `multipart/form-data` with the UTF-8 `.ics` file in the `file` part. The file may be at most 1 MB and contain at most 100 `VEVENT` entries. Previewing never writes data.

```json
{
  "filename": "interviews.ics",
  "totalEvents": 2,
  "importableEvents": 1,
  "invalidEvents": 1,
  "events": [
    {
      "eventNumber": 1,
      "title": "Technical interview",
      "details": "Panel round",
      "contact": "Zoom",
      "occurredAt": "2026-08-25T16:00:00Z",
      "suggestedType": "INTERVIEW",
      "importable": true,
      "errors": []
    }
  ]
}
```

The parser unfolds RFC 5545 continuation lines, unescapes calendar text, and reads `SUMMARY`, `DESCRIPTION`, `LOCATION`, and `DTSTART`. UTC values, named `TZID` values, floating date-times, and all-day dates are supported; floating values and all-day dates use the server clock zone. An invalid event remains visible with per-event errors while other valid events can still be selected.

### Import selected ICS calendar events

`POST /api/job-activities/calendar/import`

```json
{
  "events": [
    {
      "jobId": 4,
      "type": "INTERVIEW",
      "title": "Technical interview",
      "details": "Panel round",
      "contact": "Zoom",
      "occurredAt": "2026-08-25T16:00:00Z"
    }
  ]
}
```

The request accepts 1 to 100 events. Every event must be an `INTERVIEW` or `FOLLOW_UP` and must reference an existing job. Exact duplicates use job, type, case-insensitive title, and scheduled instant; duplicates already stored or repeated in the same request are skipped. Missing jobs return `404 Not Found`, invalid values return `400 Bad Request`, and the transaction does not leave a partial import after an error.

```json
{
  "imported": 1,
  "skippedDuplicates": 0
}
```

### List jobs needing attention

`GET /api/job-activities/needs-attention`

Returns jobs in `APPLIED`, `OA`, or `INTERVIEW` whose latest activity meets the configured threshold for that stage. For a job without activities, its creation time is used as the last touch. A future activity is treated as a scheduled next step and keeps that job out of the reminder list. A job whose `attentionSnoozedUntil` date is later than the current server-local date is also excluded.

Results are ordered by how far each job is past its own threshold, from most overdue to least overdue. Each result includes `thresholdDays`, allowing clients to explain why the reminder appeared. Success: `200 OK`; when every active application has recent or scheduled activity, the endpoint returns `[]`.

```json
[
  {
    "jobId": 1,
    "company": "OpenAI",
    "jobTitle": "Software Engineer",
    "status": "APPLIED",
    "lastActivityAt": "2026-08-10T12:00:00Z",
    "daysWithoutActivity": 13,
    "thresholdDays": 7
  }
]
```

### Read reminder change history

`GET /api/job-activities/attention-history`

Returns the 20 most recent reminder changes, newest first. Each item includes the job context, action, previous date, new date, and creation time. Actions are `SNOOZED`, `RESCHEDULED`, `RESUMED`, `RESTORED`, or `CLEARED_BY_ACTIVITY`.

```json
[
  {
    "id": 7,
    "jobId": 1,
    "company": "OpenAI",
    "jobTitle": "Software Engineer",
    "action": "RESCHEDULED",
    "previousSnoozedUntil": "2026-08-30",
    "newSnoozedUntil": "2026-09-02",
    "createdAt": "2026-08-24T12:00:00Z"
  }
]
```

Deleting a job also deletes its reminder history. A fresh history returns `[]`.

### Read follow-up reminder settings

`GET /api/job-activities/attention-settings`

Returns the persisted reminder threshold for each active pipeline stage. A fresh installation starts with seven days for every stage.

```json
{
  "appliedDays": 7,
  "onlineAssessmentDays": 7,
  "interviewDays": 7
}
```

### Update follow-up reminder settings

`PUT /api/job-activities/attention-settings`

All three fields are required integers from `1` through `90`. Success: `200 OK` with the persisted settings. Invalid values return `400 Bad Request` with field-level validation errors.

```json
{
  "appliedDays": 10,
  "onlineAssessmentDays": 5,
  "interviewDays": 3
}
```

## Resumes

Resume content is stored as plain text. A PDF or DOCX can be converted to text before the user creates a resume record.

### Extract a resume file

`POST /api/resumes/extract`

Send a `multipart/form-data` request with the PDF or DOCX in the `file` field. Files can be at most 5 MB, and extracted text can contain at most 100,000 characters.

Success: `200 OK`

```json
{
  "suggestedName": "Backend Engineer Resume",
  "content": "Experienced Java engineer focused on reliable backend systems."
}
```

Extraction does not create or update a resume record. Invalid, unreadable, unsupported, or text-free documents return `400 Bad Request`. Requests rejected by the multipart transport limit return `413 Payload Too Large`.

### Create a resume

`POST /api/resumes`

Request:

```json
{
  "name": "Backend Engineer Resume",
  "content": "Experienced Java engineer focused on reliable backend systems."
}
```

Validation rules:

- `name` and `content` are required and cannot contain only whitespace.
- `name` can contain at most 255 characters.

Success: `201 Created`

```json
{
  "id": 1,
  "name": "Backend Engineer Resume",
  "content": "Experienced Java engineer focused on reliable backend systems.",
  "createdAt": "2026-08-21T19:00:00Z"
}
```

### List resumes

`GET /api/resumes`

Returns resumes ordered from newest to oldest.

Success: `200 OK` with an array of resume responses. An empty database returns `[]`.

### Get one resume

`GET /api/resumes/{id}`

Success: `200 OK` with one resume response.

Missing resume: `404 Not Found`

```json
{
  "timestamp": "2026-08-21T19:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Resume not found with id: 999",
  "path": "/api/resumes/999",
  "fieldErrors": {}
}
```

### Update a resume

`PUT /api/resumes/{id}`

Uses the same `name` and `content` body as resume creation. Both fields are required; surrounding whitespace is removed before storage.

Success: `200 OK` with the updated resume response. The original `id` and `createdAt` are preserved.

Missing resume: `404 Not Found`

### Delete a resume

`DELETE /api/resumes/{id}`

Success: `204 No Content`. Any saved match analyses linked to the resume are also permanently deleted.

Missing resume: `404 Not Found`

## Validation errors

Invalid job, resume, and match analysis requests return `400 Bad Request`.

```json
{
  "timestamp": "2026-08-19T02:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/jobs",
  "fieldErrors": {
    "company": "Company is required"
  }
}
```

Malformed JSON and invalid path parameter types use the same error envelope with an empty `fieldErrors` object.

## Match analyses

Match analyses compare one saved job with one saved resume. The complete job description and resume content are not repeated in API responses.

### Create a match analysis

`POST /api/match-analyses`

Request:

```json
{
  "jobId": 1,
  "resumeId": 2
}
```

Both IDs are required and must be positive. Missing jobs or resumes return `404 Not Found`.

Success: `201 Created`

```json
{
  "id": 3,
  "jobId": 1,
  "company": "OpenAI",
  "jobTitle": "Software Engineer",
  "resumeId": 2,
  "resumeName": "Backend Resume",
  "matchScore": 84,
  "summary": "Strong overall match.",
  "strengths": "Relevant backend experience.",
  "gaps": "Limited cloud evidence.",
  "recommendations": "Add measurable cloud achievements.",
  "modelName": "gpt-5.6",
  "createdAt": "2026-08-22T19:00:00Z"
}
```

If the upstream AI service cannot complete the request, the API returns `502 Bad Gateway`. No failed or partial analysis is persisted, and internal provider details are not included in the response.

### List match analyses

`GET /api/match-analyses`

Returns analyses ordered from newest to oldest. Success: `200 OK`; an empty database returns `[]`.

### Get one match analysis

`GET /api/match-analyses/{id}`

Success: `200 OK` with one analysis response. Missing analysis: `404 Not Found`.

### Delete a match analysis

`DELETE /api/match-analyses/{id}`

Success: `204 No Content`. Only the saved analysis is removed; its referenced job and resume remain available.

Missing analysis: `404 Not Found`.

## Job status values

```text
SAVED
APPLIED
OA
INTERVIEW
OFFER
REJECTED
WITHDRAWN
```
