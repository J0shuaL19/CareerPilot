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

`pnpm build` produces the Windows installer and its release executable. The
desktop package includes the React frontend, Spring Boot backend, H2 database
driver, and a minimized Java 21 runtime.

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

## Web-to-desktop data migration

The Data page exports the hosted database as a versioned JSON file. Full import
is disabled in the hosted profile and enabled only in the desktop profile. The
Windows app validates the complete archive and previews its counts before a
confirmed import replaces the local H2 data in one transaction.
## Windows installer

Create a per-user NSIS installer with:

```powershell
pnpm build
```

The installer is written to `src-tauri/target/release/bundle/nsis`. It installs
without administrator access, uses the bundled Java runtime, and downloads the
Microsoft WebView2 bootstrapper only when WebView2 is unavailable. The installed
application stores its database and logs under `%LOCALAPPDATA%/CareerPilot`.
