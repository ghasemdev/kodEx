# API Contract: v1 — Project Base Setup

**Base URL**: `/api/v1`
**Authentication**: None (public endpoints only in this feature)
**Content-Type**: `application/json`

---

## Health Check

Confirms the server process is running and reachable. Used by both development tooling
and production infrastructure (Docker HEALTHCHECK, load balancers, uptime monitors,
and future Kubernetes probes).

**Performance requirement**: Must respond in < 50 ms under normal load. Must NOT touch
the database or Redis in this version — latency-free liveness check only.

### `GET /api/v1/health`

**Authentication**: None (public — must never require a token)
**Rate limiting**: Excluded from rate limiting (health checks can burst)
**Cache**: `Cache-Control: no-store`

#### Response — 200 OK

```json
{
  "data": {
    "status": "UP",
    "service": "kodex-api",
    "version": "0.1.0-SNAPSHOT",
    "startedAt": "2026-05-18T10:00:00Z"
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-05-18T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "0.1.0-SNAPSHOT"
  }
}
```

| Field (inside `data`) | Type | Notes |
|---|---|---|
| `status` | `"UP"` \| `"DOWN"` | `"UP"` when endpoint responds normally |
| `service` | String | Fixed: `"kodex-api"` |
| `version` | String | Gradle project version injected at build time |
| `startedAt` | ISO-8601 UTC | JVM process start time |

`meta.requestId` matches the correlation ID in the structured log entry for the same request.

#### Response — 503 Service Unavailable

Reserved for when dependency health checks are added (future feature). Returns when a
required dependency (DB, Redis) is unreachable and the service cannot serve traffic.

```json
{
  "data": {
    "code": "DEPENDENCY_UNAVAILABLE",
    "message": "One or more required dependencies are unavailable.",
    "userMessage": {
      "en": "The service is temporarily unavailable. Please try again shortly.",
      "fa": "سرویس موقتاً در دسترس نیست. لطفاً کمی دیگر تلاش کنید."
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-05-18T10:00:05Z",
    "service": "kodex-api",
    "serviceVersion": "0.1.0-SNAPSHOT"
  }
}
```

---

## Future: Detailed Health (authenticated, admin-only)

> Not in scope for this feature — documented here to reserve the path.

`GET /api/v1/health/detail` — requires `Authorization: Bearer <admin-token>`. Returns
dependency-level detail: DB pool state, Redis ping latency, JVM memory, etc.

---

## Standard Error Response

All errors (4xx, 5xx) across the entire API follow the unified envelope schema (Constitution §IX):

```json
{
  "data": {
    "code": "SNAKE_CASE_ERROR_CODE",
    "message": "Technical developer-facing description of what went wrong.",
    "userMessage": {
      "en": "User-friendly English message.",
      "fa": "پیام برای کاربر به فارسی."
    }
  },
  "meta": {
    "requestId": "uuid-v4-correlation-id",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "0.1.0-SNAPSHOT"
  }
}
```

`meta.requestId` matches the `requestId` field in the structured JSON log entry for the
same request, enabling log-to-response tracing in production.
`data.message` is developer-facing (safe to log). `data.userMessage` is for UI display.

### Standard Error Codes (skeleton)

| HTTP Status | Code | When |
|---|---|---|
| 400 | `INVALID_REQUEST` | Malformed JSON body |
| 404 | `NOT_FOUND` | Route does not exist |
| 405 | `METHOD_NOT_ALLOWED` | HTTP verb not supported on route |
| 500 | `INTERNAL_ERROR` | Unhandled exception |

---

## Frontend — Web Application Entry Point

### JS / WASM-JS Target Selection

The Kilua webapp is built for two targets: `js` and `wasmJs`. The HTML entry point
auto-selects the bundle at runtime:

```js
// Embedded in index.html
(async () => {
  const wasmSupported =
    typeof WebAssembly !== 'undefined' &&
    typeof WebAssembly.instantiateStreaming === 'function';
  if (wasmSupported) {
    await import('./wasmJs/kodex-webapp.mjs');
  } else {
    await import('./js/kodex-webapp.js');
  }
})();
```

| Environment | URL | Target selected |
|---|---|---|
| Development (dev server) | `http://localhost:5173` | `wasmJs` if browser supports it, else `js` |
| Production | Configured via `WEBAPP_ORIGIN` | Same runtime detection |

The webapp in this feature renders a single page with the text **"Hello KodEx"** —
a structural placeholder confirming the frontend pipeline is correctly wired for
both JS and WASM-JS build targets.

### API Proxy (Vite dev server only)

In development, Vite proxies `/api` requests to `http://localhost:8080`:

```ts
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

This avoids CORS issues during local development. Production uses CORS headers configured
via the `WEBAPP_ORIGIN` environment variable (Constitution §Security).
