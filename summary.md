# CareerPilot Project Handoff

Last verified: 2026-09-14  
Repository: `D:\DDDDD\Projects\CareerPilot`  
Branch/HEAD: `main` at `f41d96c` (`Add web-to-desktop data transfer`)

## Purpose and current scope

CareerPilot is a single-user job-search workspace. It tracks applications, resumes, recruiting activity, reminders, interview preparation, and AI-assisted job/resume match analyses. It supports both a private web deployment and a self-contained Windows desktop installation.

The current release is intentionally single-user. Public multi-user hosting, offer comparison, and AI resume optimization are not part of the present scope.

## Confirmed current state

- The web application runs as a React frontend calling a Spring Boot `/api` backend backed by PostgreSQL.
- The Windows application is a Tauri shell that starts the bundled Java backend and local H2 database, waits for `/api/health`, then shows the window. Closing the app stops the backend; a second launch focuses the existing window.
- Web-to-desktop data transfer is complete: web and desktop can export, while full import is enabled only by the desktop profile.
- The latest source revision has no known open functional bug documented in the repository. A focused TODO/FIXME scan found no actionable application TODOs.
- There is no active feature implementation in progress.
- The working tree contained one pre-existing untracked file, `commands.txt`, before this handoff. Treat it as user-owned; do not overwrite or commit it without explicit instruction.

## Architecture and technology

### Backend

- Java 21, Spring Boot 3.5.5, Maven, Spring Web, Spring Data JPA, and Bean Validation.
- Controller -> service -> repository layering under `backend/src/main/java/com/careerpilot`.
- PostgreSQL is used by local/web/production profiles; Flyway owns the schema.
- The `desktop` Maven/Spring profile includes H2 and the compiled frontend in the Spring Boot JAR.
- PDFBox and Apache POI extract PDF and DOCX resume text.
- OpenAI Responses API integration is isolated behind `ai/MatchAnalysisClient`; Structured Outputs are used and requests set `store: false`.

### Frontend

- React, TypeScript, Vite, React Router, pnpm.
- Routes and composition start in `frontend/src/App.tsx`; shared navigation is in `frontend/src/components/AppLayout.tsx`.
- API access is split by domain in `frontend/src/services`; request/response types are in `frontend/src/types`.

### Web deployment

- `compose.yaml` runs PostgreSQL, Spring Boot, and Nginx/frontend on a private Compose network.
- Only Nginx publishes a host port and proxies same-origin `/api` calls.
- Default binding is `127.0.0.1:8080`. Because the app has no authentication, deployment must remain private or sit behind an authenticated HTTPS/VPN gateway.

### Windows desktop

- Tauri 2/Rust with the Windows WebView2 runtime and a per-user NSIS installer.
- A minimized Java 21 runtime is produced with `jlink` and bundled with the Spring Boot JAR.
- Desktop data and logs are stored under `%LOCALAPPDATA%\CareerPilot`.
- The desktop backend binds to loopback and uses file-backed H2 in PostgreSQL compatibility mode.

## Important project locations

- `README.md` - product overview, local run instructions, major feature behavior.
- `docs/architecture.md` - application and production topology, AI and calendar boundaries.
- `docs/database.md` - current tables and migration rules.
- `docs/api.md` - API contract for the main product domains; data-transfer endpoints are not yet described there.
- `docs/deployment.md` - private deployment, backup/restore, upgrade, and release checklist.
- `backend/src/main/java/com/careerpilot/{controller,service,repository,model,dto}` - backend layers.
- `backend/src/main/java/com/careerpilot/ai` - OpenAI-neutral client interface and adapter.
- `backend/src/main/resources/db/migration` - immutable Flyway migrations `V1` through `V10`.
- `backend/src/main/resources/application-desktop.properties` - loopback/H2 desktop profile and desktop-only import switch.
- `frontend/src/pages`, `components`, `services`, `types` - UI screens, reusable UI, API clients, and types.
- `desktop/src-tauri/src/backend.rs` - bundled backend launch, readiness, and process shutdown.
- `desktop/src-tauri/src/lib.rs` - Tauri window and single-instance lifecycle.
- `desktop/scripts/prepare-resources.ps1` - builds the frontend/backend and bundled Java runtime.
- `.github/workflows/ci.yml` - backend tests plus frontend lint/build.

Data-transfer entrypoints:

- `backend/src/main/java/com/careerpilot/controller/DataTransferController.java`
- `backend/src/main/java/com/careerpilot/service/DataTransferService.java`
- `backend/src/main/java/com/careerpilot/dto/CareerPilotArchive.java`
- `frontend/src/pages/DataTransferPage.tsx`
- `frontend/src/services/dataTransferApi.ts`
- `frontend/src/types/dataTransfer.ts`

## Implemented features

- Dashboard funnel analytics and a daily action center.
- Searchable job pipeline, job detail/timeline, CSV preview/import, and filtered CSV export.
- Follow-up capture; configurable stage reminders; snooze, bulk actions, undo, and reminder history.
- Calendar month/week/mobile agenda views, filtering, date jump, drag rescheduling, completion/reopen, and conflict warnings.
- Filtered ICS export and guarded ICS preview/import with job mapping and duplicate detection.
- Interview preparation checklists, readiness progress, and urgent interview highlighting.
- Resume library with PDF/DOCX import, search, preview, editing, and duplication.
- AI job/resume match analysis and saved searchable analysis history.
- Versioned full-data JSON export and transactional desktop import.
- Private Docker Compose deployment and Windows NSIS packaging.

## Data-transfer behavior and constraints

- Endpoints: `GET /api/data-transfer/capabilities`, `GET /export`, `POST /import/preview`, and `POST /import`.
- Archive format is `careerpilot-data`, version `1`.
- Exports include jobs, resumes, activities/calendar data, attention settings/history, match analyses, and interview preparation.
- API keys, database passwords, and other runtime credentials are excluded.
- Import validates the complete archive before writing, then replaces local data in one transaction; it does not merge.
- Import is disabled by default and enabled only in `application-desktop.properties`.
- Maximum archive size is 20 MiB; multipart requests are capped at 25 MB; each section is capped at 20,000 records.

## Important decisions and reasons

- **Tauri instead of Electron:** reuses Windows WebView2 and avoids bundling another Chromium runtime, keeping the Java-based desktop package lighter.
- **Keep the Java backend:** web and desktop share the same domain logic, validation, persistence mappings, and API.
- **PostgreSQL for web, H2 for desktop:** PostgreSQL provides durable server persistence; embedded H2 enables a single-user installation without a separate database service.
- **Desktop-only full import:** hosted data cannot be accidentally replaced through the web deployment.
- **Transactional replacement instead of merge:** produces deterministic migration and avoids ambiguous conflict resolution.
- **Flyway-only schema evolution:** never edit an already-applied migration; add a new migration.
- **Private single-user release:** authentication and per-user isolation were deliberately deferred, so network access is an operational boundary.
- **AI adapter boundary:** business services do not depend on OpenAI-specific wire types.

## Run, test, and build commands

Local backend (requires JDK 21, Maven, PostgreSQL, and the variables in `.env.example`):

```powershell
cd backend
mvn spring-boot:run
mvn test
```

Local frontend (Node.js 20+; CI currently uses Node 24 and pnpm 11.19.0):

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm run dev
pnpm run lint
pnpm run build
```

Private web deployment, from the repository root:

```powershell
docker compose config
docker compose build --pull
docker compose up -d
docker compose ps
```

Windows desktop, from `desktop`:

```powershell
pnpm install --frozen-lockfile
pnpm dev
pnpm build
cargo test --manifest-path src-tauri/Cargo.toml
```

`pnpm build` recreates generated resources and writes the installer under `desktop/src-tauri/target/release/bundle/nsis`.

Never commit `.env`, passwords, API keys, generated desktop resources, local databases, or logs.

## Recent significant changes

- `f41d96c` - web export plus validated, transactional desktop import and Data page.
- `3b9df10` - per-user Windows NSIS distribution.
- `915e81f` - desktop backend startup/readiness/shutdown and single-instance lifecycle.
- `45f8d79` - bundled minimized Java runtime build pipeline.
- `48a9ac5` - desktop Spring profile and local H2 persistence.
- `26d6c85` - initial Tauri desktop foundation.
- `66fc457` - private production deployment preparation.
- `8361157` through `a7abf33` - calendar interview readiness, preparation, guarded import, filtered export, and drag rescheduling.

At `f41d96c`, the last full validation recorded:

- Backend: 260 tests passed.
- Frontend: lint and production build passed.
- Desktop Rust: 4 tests passed.
- Installed EXE smoke test passed export -> preview -> import -> read-back, excluded the API key, and confirmed both app and Java backend exited.
- Installer: `CareerPilot_0.1.0_x64-setup.exe`, 117,045,034 bytes, SHA-256 `B4BFDF861420050502B15E5EB1375995B08245A47D5BB7E8AD87B11E8F792C4B`.

## Unresolved issues and risks

- The installer is not code-signed, so Windows SmartScreen warnings are expected. Signing is the main release-distribution gap.
- There is no login or per-user data isolation. Do not expose this release directly to the public internet.
- Import is intentionally destructive after confirmation. Version 1 has no cross-version migration strategy beyond rejecting unsupported archives; keep backups.
- `DataTransferService` is large and centralizes validation, deletion, and recreation. Split it only when real new requirements justify the added seams.
- `docs/api.md` does not yet document the data-transfer endpoints.
- Browser notifications work only while CareerPilot is open and require permission plus a secure browser context.
- Frontend `package.json` uses several `latest` ranges. The lockfile stabilizes current installs, but dependency upgrades should be deliberate and verified.

No other confirmed open defect was found during this handoff inspection.

## Deferred or proposed work

Explicitly deferred unless the user reprioritizes it:

- Offer management and offer comparison.
- Login, accounts, and per-user data isolation.
- AI resume optimization.

Recommended next steps, in priority order:

1. Decide the next release target before coding: private web deployment or distributable Windows release.
2. For Windows distribution, obtain a code-signing certificate and add a repeatable signing/release checklist.
3. Add automated end-to-end coverage for packaged startup/shutdown and web-export-to-desktop-import before expanding the archive format.
4. Document the four data-transfer endpoints in `docs/api.md` and define a policy for future archive versions.
5. Add authentication and user ownership only if public or multi-user deployment becomes a real requirement.
6. Revisit offer comparison or AI resume optimization only after the release priorities above are settled.

## Do not repeat unnecessarily

- Do not redesign the desktop shell with Electron unless package size, WebView2 compatibility, or another measured requirement changes.
- A Windows Job Object process guard was tried during desktop lifecycle investigation, added complexity, and was removed. Normal close already terminates the Java child; reproduce a real lifecycle failure before revisiting it.
- Earlier smoke-test false alarms came from checking `ok` instead of the actual health value `UP`, reading `data.jobs` instead of the export's actual structure, reading the wrong preview field instead of `incoming.jobs`, and fragile process matching. Inspect real response schemas and logs before diagnosing a product failure.
- Do not rebuild the data-transfer feature from scratch. Its backend, frontend, tests, installer, and installed-app round trip were already completed and verified at `f41d96c`.
- Do not edit existing Flyway migrations or commit the local `.env`/`commands.txt` files.
