# API design

## Foundation endpoint

### `GET /api/health`

Confirms that the backend is running.

```json
{
  "status": "ok",
  "service": "careerpilot-backend"
}
```

Job and resume endpoints will be defined with their implementation in later milestones.
