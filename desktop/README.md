# CareerPilot Desktop

This directory contains the Windows desktop shell. The existing React frontend
and Spring Boot backend stay independent and continue to support the web build.

## Commands

Run commands from this directory:

```powershell
pnpm install
pnpm dev
pnpm build
```

The first milestone only embeds the React production build. The bundled Java
backend, local H2 database, startup health check, and process cleanup are added
in the following milestones.

## Desktop backend

Build the frontend first, then package the backend with the `desktop` Maven
profile:

```powershell
pnpm --dir ../frontend build
mvn --file ../backend/pom.xml --activate-profiles desktop package
```

The profile packages the React build and H2 driver into the Spring Boot JAR.
When run with `--spring.profiles.active=desktop`, it binds only to loopback and
stores data in `${user.home}/.careerpilot/data` by default. The Tauri launcher
will override the data and log paths with `%LOCALAPPDATA%/CareerPilot`.
## Bundled resources

Prepare the backend and a minimized Java 21 runtime with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/prepare-resources.ps1
```

`pnpm build` runs this script automatically before Tauri creates a Windows
bundle. Generated JAR and runtime files live under `src-tauri/resources` and are
ignored by Git; the script always recreates them from the committed sources.
## Application lifecycle

The desktop launcher keeps the main window hidden while it starts the bundled
backend. After `/api/health` confirms both the service and database are ready,
the window navigates to the loopback URL and becomes visible. Application data
is stored under `%LOCALAPPDATA%/CareerPilot`; closing the app also stops the
backend process. A second launch focuses the existing window.
