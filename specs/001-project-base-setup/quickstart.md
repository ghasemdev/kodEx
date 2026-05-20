# Quick Start: KodEx Project Foundation

**Feature**: Project Base Setup | **Date**: 2026-05-18

---

## Prerequisites

| Tool                                           | Minimum Version | Purpose                            |
|------------------------------------------------|-----------------|------------------------------------|
| Docker Desktop (or Docker Engine + Compose v2) | Latest stable   | Run all services                   |
| JDK                                            | **21** (LTS)    | Build and run JVM services locally |
| Git                                            | Any recent      | Source control                     |

> Everything else (Gradle wrapper, Node.js for Vite) is downloaded automatically on first
> build. No global `npm`, `node`, or separate Gradle installation required.

---

## 1 — Clone & Configure

```bash
git clone https://github.com/ghasemdev/kodEx.git
cd kodEx

# Copy environment template
cp .env.example .env

# (Optional) Edit .env if you need non-default ports or credentials
# nano .env
```

---

## 2 — Start All Services (Docker)

```bash
docker compose -f docker/docker-compose.yml up --build
```

This command:

- Builds the server and webapp Docker images
- Starts PostgreSQL and Redis
- Starts the Ktor API server on port **8080**
- Starts the Vite dev server (webapp) on port **5173**

All services log to stdout with structured JSON (formatted for readability in local dev).

---

## 3 — Verify

**Web page**: Open [http://localhost:5173](http://localhost:5173) in a browser.
Expected: A page displaying **"Hello KodEx"**.

**API**: In a terminal:

```bash
curl -s http://localhost:8080/api/v1/health | jq
```

Expected response:

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

## 4 — Run Quality Checks Locally

```bash
# Run all tests + generate coverage report
./gradlew test koverXmlReport

# Run static analysis
./gradlew detekt

# Run everything (build + test + quality)
./gradlew build detekt
```

Coverage report: `build/reports/kover/report.xml`
HTML report: `build/reports/kover/html/index.html`
Detekt report: `build/reports/detekt/detekt.html`

---

## 5 — Frontend Development (hot-reload)

```bash
# Start only the backend (Docker or local JVM)
./gradlew :server:app:run

# Start Kilua/Vite dev server with HMR in a second terminal
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

The Vite dev server proxies `/api` requests to the backend on port 8080, so both can run
simultaneously.

---

## 6 — Stop Services

```bash
docker compose -f docker/docker-compose.yml down
```

Add `-v` to also remove the PostgreSQL and Redis data volumes:

```bash
docker compose -f docker/docker-compose.yml down -v
```

---

## Troubleshooting

| Problem                         | Cause                      | Fix                                                   |
|---------------------------------|----------------------------|-------------------------------------------------------|
| `Port 8080 already in use`      | Another process using 8080 | Change `SERVER_PORT` in `.env`                        |
| `Port 5173 already in use`      | Another Vite/dev server    | Change Vite port in `app/webApp/vite.config.ts`       |
| `Missing prerequisite: JDK 21`  | Wrong JDK version          | Install [Temurin JDK 21](https://adoptium.net/)       |
| Docker build fails on first run | Dependencies downloading   | Re-run; first build downloads ~500 MB of dependencies |
| `.env not found`                | Forgot step 1              | Run `cp .env.example .env`                            |
