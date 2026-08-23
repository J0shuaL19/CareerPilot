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
  "createdAt": "2026-08-19T02:00:00Z"
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
    "createdAt": "2026-08-19T02:00:00Z"
  }
]
```

An empty database returns `[]`.

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

Returns the job's activities ordered by occurrence time from newest to oldest. Success: `200 OK`; a job without activity returns `[]`. A missing job returns `404 Not Found`.

### Update a job activity

`PUT /api/jobs/{jobId}/activities/{activityId}`

Uses the same request body and validation rules as activity creation. Success: `200 OK` with the complete updated activity. The activity must belong to the job in the request path; missing jobs or activities return `404 Not Found`.

### Delete a job activity

`DELETE /api/jobs/{jobId}/activities/{activityId}`

Success: `204 No Content`. The activity must belong to the job in the request path. Missing jobs or activities return `404 Not Found`.

### List upcoming job activities

`GET /api/job-activities/upcoming`

Returns interviews and follow-ups scheduled from the current server time through the next 14 days, ordered from nearest to latest. Past activities, applications, and general notes are excluded.

Each response includes `jobId`, `company`, and `jobTitle` so the frontend can present reminders across the complete pipeline. Success: `200 OK`; no upcoming reminders returns `[]`.

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
