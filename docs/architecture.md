# Architecture

CareerPilot uses a single repository with independently runnable frontend and backend applications.

```text
React frontend -> HTTP /api -> Spring Boot backend -> PostgreSQL (next phase)
                                             |
                                             +-> OpenAI API (later phase)
```

The backend follows controller -> service -> repository layering. AI integration remains isolated in the `ai` package.
