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
