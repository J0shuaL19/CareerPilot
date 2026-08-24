# Architecture

CareerPilot uses a single repository with independently runnable frontend and backend applications.

```text
React frontend -> HTTP /api -> Spring Boot backend -> PostgreSQL
                                             |
                                             +-> AI client interface -> OpenAI Responses API
```

The backend follows controller -> service -> repository layering. AI integration remains isolated in the `ai` package behind `MatchAnalysisClient`, so business services do not depend on OpenAI-specific HTTP types.

Calendar reads use an explicit date range of at most 62 days. `JobActivityService` loads scheduled activities first, then loads preparation rows for all interview activity IDs in one repository query and joins them in memory, avoiding N+1 reads. The React calendar keeps the returned schedule as its single state source, so checklist saves can update the readiness panel, grid, agenda, and details without reloading the full range.

The OpenAI adapter uses Responses API Structured Outputs with a strict JSON Schema. Resume and job content are sent with `store: false`. Runtime configuration comes from `OPENAI_API_KEY`, `OPENAI_BASE_URL`, and `OPENAI_MODEL` environment variables.
