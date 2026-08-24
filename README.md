# CareerPilot

CareerPilot is an AI-powered job search workspace for tracking applications, maintaining resumes, recording recruiting activity, and comparing jobs with a resume.

## Technology stack

- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Database: PostgreSQL with Flyway migrations
- AI: OpenAI Responses API through an isolated backend adapter

## Repository structure

```text
CareerPilot/
├── backend/    Spring Boot API and business logic
├── frontend/   React user interface
└── docs/       Architecture, database, and API notes
```

Current features include a career dashboard with time-range funnel analytics and a unified daily action center for due follow-ups and upcoming activities with completion outcomes and optional stage updates, configurable stage-specific reminders with per-job snoozing, opt-in browser alerts, transactional bulk reminder actions with one-click undo, and a recent reminder change history, quick follow-up capture, and personalized follow-up message templates with one-click copy, searchable job pipeline tracking with validated CSV import and filtered export, editable job activity timelines with calendar export, PDF/DOCX resume import, searchable resume previews and version duplication, AI match analysis, and searchable saved analysis history.

## Run locally

### Backend

Requirements: JDK 21, Maven, and PostgreSQL.

Create a PostgreSQL database and user, then expose the connection values as environment variables. See `.env.example` and `docs/database.md` for the expected names.

```bash
cd backend
mvn spring-boot:run
```

The backend health endpoint is available at `http://localhost:8080/api/health`.

### Frontend

Requirements: Node.js 20 or newer and pnpm.

```bash
cd frontend
pnpm install
pnpm run dev
```

The Vite development server is available at `http://localhost:5173`.

## Daily action center

The dashboard combines follow-ups that have reached their stage-specific reminder threshold with interviews and follow-ups scheduled in the next 14 days. Today's scheduled activities appear first, followed by due application follow-ups ordered by urgency, then later activities in chronological order. Each row keeps its relevant quick actions: follow up, snooze, or open the job.

## Browser reminder alerts

The dashboard can show browser notifications for due applications in the daily action center. Alerts are opt-in and only run while CareerPilot is open in a secure browser context. Each overdue job is notified at most once per local calendar day; the preference and daily delivery record stay in that browser's local storage.

If notification permission is blocked, use the browser's site settings to allow notifications and then return to CareerPilot. Turning alerts off stops future notifications without changing reminder rules, snoozes, or server data.
