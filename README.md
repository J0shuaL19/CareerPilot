# CareerPilot

CareerPilot is an AI-powered job search platform that will help users compare job descriptions with a resume and track applications through the recruiting pipeline.

## Technology stack

- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Database (next phase): PostgreSQL
- AI (later phase): OpenAI API through the backend

## Repository structure

```text
CareerPilot/
├── backend/    Spring Boot API and business logic
├── frontend/   React user interface
└── docs/       Architecture, database, and API notes
```

This initial milestone contains only runnable backend and frontend foundations. Job tracking, PostgreSQL, and AI integration intentionally belong to later milestones.

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

Requirements: Node.js 20 or newer.

```bash
cd frontend
npm install
npm run dev
```

The Vite development server is available at `http://localhost:5173`.
