# API design

Base URL during local development: `http://localhost:8080`

All request and response bodies use JSON. Dates use ISO 8601 UTC timestamps.

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

## Validation errors

Invalid requests return `400 Bad Request`.

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
