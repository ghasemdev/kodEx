# Quickstart: Landing Page (003)

**Branch**: `feature/003-landing-page`

---

## Prerequisites

Same as spec 002. See `specs/002-design-system/quickstart.md` for Docker + env setup.

Additional requirement: Chrome (or Chromium) on PATH for screenshot tests.
Set `CHROME_BIN` in `local.properties` or as an env var (see spec 002 quickstart).

---

## Running the Frontend (Landing Page)

```bash
# Start the Vite dev server (JS target, HMR enabled)
./gradlew :app:webApp:jsViteRun

# Open in browser
open http://localhost:5173
```

The landing page renders at `/`. The playground is still available at `/playground`.

---

## Running the Backend Stats Endpoint

```bash
# Start the Ktor server (devRun includes seed data + human-readable logs)
./gradlew :server:app:devRun

# Verify the stats endpoint
curl http://localhost:8080/api/v1/stats/landing
# → {"data":{"totalProblems":1247,"totalUsers":8432,"totalContests":342},"meta":{...}}
```

The frontend Vite dev server proxies `/api/*` to `http://localhost:8080` automatically
(Vite proxy config in `app/webApp/vite.config.ts`).

---

## Running Tests

```bash
# Unit + component tests (JS target, headless Chrome)
CHROME_BIN="..." ./gradlew :app:webApp:jsBrowserTest

# Screenshot tests only
CHROME_BIN="..." ./gradlew :app:webApp:jsBrowserTest --tests "*.LandingPageScreenshotTest"

# WASM target tests
./gradlew :app:webApp:wasmJsBrowserTest
```

---

## GSAP Smoke Test (playground)

After adding the GSAP npm dependency (T001), verify it loads correctly:

```bash
./gradlew :app:webApp:jsViteRun
open http://localhost:5173/playground
# Select "GSAP Smoke Test" from the playground registry
# A box should slide in from the left over 1 second
```

---

## Checking the Animation

1. Open `http://localhost:5173`
2. The hero section should auto-play the Kotlin tab sequence immediately
3. After ~10s the Android tab should activate automatically
4. Click the Kotlin / Android tabs to replay either sequence manually
5. To test reduced-motion: open DevTools → Rendering → set "Emulate CSS media feature
   prefers-reduced-motion" to `reduce` → reload the page → all sections appear instantly

---

## Environment Variables

No new env vars required for the landing page.
The stats endpoint uses no secrets — it returns hardcoded values in v1.
