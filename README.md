# KodEx

An online programming exam platform with Kotlin-first stack, secure code execution, and real-time grading.

---

## Prerequisites

| Tool                                           | Minimum Version | Purpose                            |
|------------------------------------------------|-----------------|------------------------------------|
| Docker Desktop (or Docker Engine + Compose v2) | Latest stable   | Run all services                   |
| JDK                                            | **21** (LTS)    | Build and run JVM services locally |
| Git                                            | Any recent      | Source control                     |

> Everything else (Gradle wrapper, Node.js for Vite) is downloaded automatically on first build.

---

## 1 — Clone & Configure

```bash
git clone https://github.com/ghasemdev/kodEx.git
cd kodEx

cp .env.example .env
# Edit .env if you need non-default ports or credentials
```

---

## 2 — Start All Services

```bash
docker compose -f docker/docker-compose.yml up --build
```

This command:

- Builds the server and sandbox-runner Docker images
- Starts PostgreSQL 17 and Redis 7
- Starts the Ktor API server on port **8080**
- Starts the Vite dev server (webapp) on port **5173**

All services log to stdout.

---

## 3 — Verify

**Web page**: Open [http://localhost:5173](http://localhost:5173) — expected: **"Hello KodEx"**

**API health check**:

```bash
curl -s http://localhost:8080/api/v1/health | jq
```

Expected:

```json
{
  "data": {
    "status": "UP",
    "startedAt": "2026-05-20T10:00:00Z"
  },
  "meta": {
    "requestId": "...",
    "timestamp": "...",
    "service": "kodex-api",
    "serviceVersion": "0.1.0-SNAPSHOT"
  }
}
```

---

## 4 — Quality Checks

```bash
# Tests + coverage report
./gradlew test koverXmlReport

# Static analysis
./gradlew detekt

# Full build
./gradlew build detekt
```

Reports:

- Coverage XML: `build/reports/kover/report.xml`
- Coverage HTML: `build/reports/kover/html/index.html`
- Detekt HTML: `build/reports/detekt/detekt.html`

---

## 5 — Frontend Development (hot-reload)

```bash
# Terminal 1 — backend
./gradlew :server:app:run

# Terminal 2 — frontend with HMR
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

Vite proxies `/api` requests to `http://localhost:8080`.

---

## 6 — Stop Services

```bash
docker compose -f docker/docker-compose.yml down

# Also remove data volumes
docker compose -f docker/docker-compose.yml down -v
```

---

## Troubleshooting

| Problem                         | Cause                              | Fix                                             |
|---------------------------------|------------------------------------|-------------------------------------------------|
| `Port 8080 already in use`      | Another process on 8080            | Change `SERVER_PORT` in `.env`                  |
| `Port 5173 already in use`      | Another Vite/dev server            | Change Vite port in `app/webApp/vite.config.ts` |
| `Missing prerequisite: JDK 21`  | Wrong JDK version                  | Install [Temurin JDK 21](https://adoptium.net/) |
| Docker build fails on first run | Dependencies downloading (~500 MB) | Re-run; cache warms after first build           |
| `.env not found`                | Forgot step 1                      | Run `cp .env.example .env`                      |

---

## Branch Protection (post-CI setup)

After the first successful CI run, configure branch protection in **Settings → Branches**:

- Add rules for `develop` and `main`
- Require status checks: `ci / build`, `ci / test`, `ci / quality`
- Require branches to be up to date before merging
- Dismiss stale pull request approvals on push

> Must be configured by a repository admin.
