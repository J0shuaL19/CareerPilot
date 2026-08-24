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

Migrations: `V1__create_jobs_table.sql`, `V6__add_job_attention_snooze.sql`

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `company` | `VARCHAR(255)` | Employer name |
| `title` | `VARCHAR(255)` | Position title |
| `description` | `TEXT` | Full job description |
| `job_url` | `VARCHAR(2048)` | Optional source URL |
| `status` | `VARCHAR(32)` | Application pipeline status |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | UTC creation time |
| `attention_snoozed_until` | `DATE` | Optional date until which the dashboard reminder is hidden |

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

## Job activities table

Migrations: `V4__create_job_activities_table.sql`, `V8__add_job_activity_completion.sql`, `V9__add_job_activity_completion_status_snapshot.sql`

Each row records one dated event or note in a job's application history. Deleting the owning job removes its activities.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `job_id` | `BIGINT` | Owning job |
| `type` | `VARCHAR(32)` | Application, interview, follow-up, or note |
| `title` | `VARCHAR(255)` | Short activity summary |
| `details` | `TEXT` | Optional notes and next steps |
| `contact` | `VARCHAR(255)` | Optional recruiter or interviewer |
| `occurred_at` | `TIMESTAMP WITH TIME ZONE` | When the activity happened or is scheduled |
| `completed_at` | `TIMESTAMP WITH TIME ZONE` | Optional UTC time when a scheduled interview or follow-up was completed |
| `completion_note` | `TEXT` | Optional completion outcome, limited to 2000 characters |
| `completion_previous_job_status` | `VARCHAR(32)` | Job stage before completion changed it; paired with the applied snapshot |
| `completion_applied_job_status` | `VARCHAR(32)` | Stage applied during completion, used to avoid overwriting later manual changes on reopen |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | UTC record creation time |

## Interview preparations table

Migration: `V10__create_interview_preparations_table.sql`

Each interview activity can own one persistent four-part preparation checklist. The unique activity foreign key enforces one checklist per interview, and deleting the activity removes its checklist.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `activity_id` | `BIGINT` | Unique owning interview activity |
| `company_research` | `TEXT` | Optional company and market notes |
| `company_research_done` | `BOOLEAN` | Company research completion flag |
| `role_priorities` | `TEXT` | Optional role priority notes |
| `role_priorities_done` | `BOOLEAN` | Role review completion flag |
| `star_stories` | `TEXT` | Optional STAR story notes |
| `star_stories_done` | `BOOLEAN` | STAR rehearsal completion flag |
| `questions_to_ask` | `TEXT` | Optional interviewer questions |
| `questions_to_ask_done` | `BOOLEAN` | Question preparation completion flag |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Last checklist save time |

## Job attention events table

Migration: `V7__create_job_attention_events_table.sql`

Each row records one reminder state change for one job. Bulk operations create one row per selected job so every original date remains independently traceable. Deleting the owning job removes its reminder events.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Database-generated identifier |
| `job_id` | `BIGINT` | Owning job |
| `action` | `VARCHAR(32)` | Pause, reschedule, resume, restore, or activity-triggered clear |
| `previous_snoozed_until` | `DATE` | Optional date before the change |
| `new_snoozed_until` | `DATE` | Optional date after the change |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | UTC event creation time |

At least one of the previous or new dates must be present.

## Job attention settings table

Migration: `V5__create_job_attention_settings_table.sql`

A single row stores the reminder timing used by the dashboard. The seeded row uses identifier `1` and preserves the original seven-day behavior until the user customizes it.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | `BIGINT` | Stable singleton identifier |
| `applied_days` | `INTEGER` | Reminder threshold for submitted applications |
| `online_assessment_days` | `INTEGER` | Reminder threshold while waiting after an assessment |
| `interview_days` | `INTEGER` | Reminder threshold after interviews or recruiter calls |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Last settings update time |

Each threshold is constrained to `1` through `90`.

Future schema changes must be added as new Flyway migrations. Do not edit a migration after it has been applied to a shared database.
