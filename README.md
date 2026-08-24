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

Current features include a career dashboard with time-range funnel analytics and a unified daily action center for due follow-ups, overdue activity recovery with quick rescheduling, a responsive month/week activity calendar with date jumping, drag-and-drop rescheduling, filtered ICS export, guarded ICS import preview, a mobile agenda, and interview conflict warnings, and upcoming activities with completion outcomes, optional stage updates, and safe one-click reopen, configurable stage-specific reminders with per-job snoozing, opt-in browser alerts, transactional bulk reminder actions with one-click undo, and a recent reminder change history, quick follow-up capture, and personalized follow-up message templates with one-click copy, searchable job pipeline tracking with validated CSV import and filtered export, editable job activity timelines with calendar export, PDF/DOCX resume import, searchable resume previews and version duplication, AI match analysis, and searchable saved analysis history.

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

## Activity calendar

The Calendar page organizes interviews and follow-ups into a desktop calendar grid and a date-grouped mobile agenda. Switch between the 42-day month view and a focused 7-day week, move one period at a time, or jump directly to a date. Search the current view by activity, company, role, or contact, and combine type, completion status, and job filters to focus the schedule. Export the exact activities shown by those filters as one standards-compliant ICS file for Google Calendar, Outlook, or Apple Calendar. External UTF-8 ICS files can be previewed before writing: CareerPilot identifies valid and invalid events, suggests interview or follow-up types, lets each selected event be mapped to a job, and skips exact duplicates during confirmation. Use the page-level action, a desktop date number, or a mobile agenda date to choose a job and add a pre-dated interview or follow-up without leaving the calendar. On desktop, drag any unfinished activity card onto another day to preserve its original time and open a confirmation step before saving. When scheduling or rescheduling an interview, CareerPilot checks the surrounding hour for unfinished activities and shows a non-blocking warning; completed activities and the activity being moved are ignored. Open any activity to review its schedule, contact, job stage, and status; unfinished work can be completed or rescheduled, completed work can be safely reopened, and the related job remains one click away. Completed activities stay visible as muted history. Each navigation mode loads an explicit bounded date range so the view is not limited by the dashboard's 14-day window.

## Browser reminder alerts

The dashboard can show browser notifications for due applications in the daily action center. Alerts are opt-in and only run while CareerPilot is open in a secure browser context. Each overdue job is notified at most once per local calendar day; the preference and daily delivery record stay in that browser's local storage.

If notification permission is blocked, use the browser's site settings to allow notifications and then return to CareerPilot. Turning alerts off stops future notifications without changing reminder rules, snoozes, or server data.
