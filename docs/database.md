# Database design

CareerPilot uses PostgreSQL in production and Flyway for versioned schema migrations. Hibernate validates the mapped entities but does not create or modify tables.

## Local connection

The backend reads these environment variables:

```text
DB_URL=jdbc:postgresql://localhost:5432/careerpilot
DB_USERNAME=careerpilot
DB_PASSWORD=<your local password>
```

The values shown in `.env.example` are documentation; Spring Boot reads the actual operating-system environment variables.

## Jobs table

Migration: `V1__create_jobs_table.sql`

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `company` | `VARCHAR(255)` | Employer name |
| `title` | `VARCHAR(255)` | Position title |
| `description` | `TEXT` | Full job description |
| `job_url` | `VARCHAR(2048)` | Optional source URL |
| `status` | `VARCHAR(32)` | Application pipeline status |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | UTC creation time |

## Resumes table

Migration: `V2__create_resumes_table.sql`

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `name` | `VARCHAR(255)` | User-facing resume name |
| `content` | `TEXT` | Plain-text resume content used by later analysis |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | UTC creation time |

## Match analyses table

Migration: `V3__create_match_analyses_table.sql`

Each row stores one completed comparison between a saved job and resume. Analysis text is separated into stable sections so the API and frontend do not depend on provider-specific response formats.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `job_id` | `BIGINT` | Referenced job; deleting the job removes its analyses |
| `resume_id` | `BIGINT` | Referenced resume; deleting the resume removes its analyses |
| `match_score` | `INTEGER` | Overall match score constrained to `0` through `100` |
| `summary` | `TEXT` | Concise overall assessment |
| `strengths` | `TEXT` | Resume evidence aligned with the job |
| `gaps` | `TEXT` | Missing or weakly demonstrated requirements |
| `recommendations` | `TEXT` | Concrete resume improvement suggestions |
| `model_name` | `VARCHAR(100)` | AI model that generated the result |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | UTC creation time |

Future schema changes must be added as new Flyway migrations. Do not edit a migration after it has been applied to a shared database.
