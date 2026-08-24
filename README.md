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

Current features include a career dashboard with time-range funnel analytics, configurable stage-specific follow-up reminders with per-job snoozing and transactional bulk snooze management, quick follow-up capture, and personalized follow-up message templates with one-click copy, searchable job pipeline tracking with validated CSV import and filtered export, editable job activity timelines with calendar export, PDF/DOCX resume import, searchable resume previews and version duplication, AI match analysis, and searchable saved analysis history.

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
