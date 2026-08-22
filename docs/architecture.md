# Architecture

CareerPilot uses a single repository with independently runnable frontend and backend applications.

```text
React frontend -> HTTP /api -> Spring Boot backend -> PostgreSQL
                                             |
                                             +-> AI client interface -> OpenAI Responses API
```

The backend follows controller -> service -> repository layering. AI integration remains isolated in the `ai` package behind `MatchAnalysisClient`, so business services do not depend on OpenAI-specific HTTP types.

The OpenAI adapter uses Responses API Structured Outputs with a strict JSON Schema. Resume and job content are sent with `store: false`. Runtime configuration comes from `OPENAI_API_KEY`, `OPENAI_BASE_URL`, and `OPENAI_MODEL` environment variables.
