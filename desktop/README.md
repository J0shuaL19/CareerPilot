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