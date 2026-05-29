# Contract: Landing Stats API

**Endpoint**: `GET /api/v1/stats/landing`
**Auth**: None (fully public)
**Cache**: `Cache-Control: public, max-age=300`

---

## Request

```
GET /api/v1/stats/landing
```

No query parameters. No request body. No authentication header required.

---

## Response — 200 OK

Follows the §IX unified `{data, meta}` envelope.

```json
{
  "data": {
    "totalProblems": 1247,
    "totalUsers": 8432,
    "totalContests": 342
  },
  "meta": {
    "requestId": "018f1c3a-2b4d-7e8f-9a0b-1c2d3e4f5a6b",
    "service": "kodex-api",
    "version": "0.1.0",
    "timestamp": "2026-05-29T10:00:00Z"
  }
}
```

### `data` fields

| Field | Type | Description |
|-------|------|-------------|
| `totalProblems` | `integer` | Total problems in the catalogue |
| `totalUsers` | `integer` | Total registered users on the platform |
| `totalContests` | `integer` | Total contests ever created |

---

## Response — 500 Internal Server Error

```json
{
  "error": {
    "message": "An unexpected error occurred.",
    "userMessage": {
      "en": "Something went wrong. Please try again later.",
      "fa": "مشکلی پیش آمد. لطفاً دوباره تلاش کنید."
    }
  },
  "meta": {
    "requestId": "018f1c3a-2b4d-7e8f-9a0b-1c2d3e4f5a6b",
    "service": "kodex-api",
    "version": "0.1.0",
    "timestamp": "2026-05-29T10:00:00Z"
  }
}
```

---

## Frontend Handling

On stats fetch failure, the hero counters show `"—"` instead of numbers.
The rest of the landing page remains fully functional.
The error is logged at `WARN` level — no user-visible error banner.

---

## v1 Implementation Note

v1 returns hardcoded values. Real DB aggregation (`COUNT` queries on Users, Contests,
Problems tables) will replace the hardcoded values incrementally as those tables are
created in specs 004–007.
