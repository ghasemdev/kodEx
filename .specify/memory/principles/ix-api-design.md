# §IX — API Design Conventions

All backend HTTP endpoints MUST follow these conventions:

- **URL prefix**: every endpoint lives under `/api/v1/`. Future versions use `/api/v2/`, etc.
- **HTTP semantics**:
  - `POST` → 201 Created (resource created) or 200 OK (action without new resource)
  - `GET` → 200 OK
  - `PUT` / `PATCH` → 200 OK
  - `DELETE` → **200 OK** with `data: null` (see envelope below — 204 is not used)
  - Client errors → 4xx; server errors → 5xx

## Unified Response Envelope

Every API response — success **and** error — MUST be wrapped in the following envelope.
The `data` field carries the resource (or `null` for DELETE / actions with no return value).
The `meta` field is always present and carries correlation and service information.

**Success response** (all 2xx responses):
```json
{
  "data": "<resource or list or null>",
  "meta": {
    "requestId": "uuid-v4",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "1.0.0"
  }
}
```

**Error response** (all 4xx and 5xx responses):
```json
{
  "data": {
    "code": "SNAKE_CASE_ERROR_CODE",
    "message": "Technical developer-facing description",
    "userMessage": {
      "en": "User-friendly English message",
      "fa": "پیام برای کاربر به فارسی"
    }
  },
  "meta": {
    "requestId": "uuid-v4",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "1.0.0"
  }
}
```

`requestId` MUST match the correlation ID in the structured log entry for the same request
(see §VI). `message` is for developers/logs; `userMessage` is for UI display in both
supported languages (English and Persian).

**Paginated list response** — pagination metadata lives inside `meta.pagination`:
```json
{
  "data": ["..."],
  "meta": {
    "requestId": "uuid-v4",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "1.0.0",
    "pagination": {
      "nextCursor": "opaque-cursor-string",
      "hasMore": true
    }
  }
}
```

Pagination uses **cursor-based** strategy for lists > 100 items. Query params: `cursor`, `limit`.
`pagination` key is omitted from `meta` when the response is not a paginated list.

## Async Result Delivery

Submission grading is asynchronous (Docker execution time varies). The API MUST expose a
**Server-Sent Events (SSE)** stream for submission status updates:

```
GET /api/v1/submissions/{id}/status
Accept: text/event-stream
```

Event sequence: `QUEUED` → `RUNNING` → `SCORED` | `FAILED`

Each SSE event carries a JSON payload, e.g.:
```
data: {"status":"RUNNING"}
data: {"status":"SCORED","score":85,"passedTests":8,"totalTests":10}
```

The stream MUST close automatically when a terminal state (`SCORED` or `FAILED`) is reached.
Clients that reconnect after a terminal state MUST receive the final state immediately (no
re-execution). The Ktor `ktor-server-sse` plugin MUST be used for SSE support.

**Rationale**: SSE is simpler to implement and scale than WebSocket for this one-directional
use case, and is natively supported by browsers without extra libraries. Consistent API shape
means the frontend and future clients can share a single HTTP client layer.
