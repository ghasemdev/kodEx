# Worklog

Concise high-value entries only. This is NOT a changelog.
Only record durable lessons — what future work should know, not what was done.

---

### 2026-05-20 - W1: Feature 001 complete — KodEx project foundation

**Milestone**: `feature/001-project-base-setup` → ready for merge to `develop`

**What future work needs to know**:
- Gradle convention plugins live in `build-logic/` — before adding a dependency to a module, check the relevant convention plugin first; it may already provide the dependency transitively
- `core:models` is the single source of truth for shared domain models — no other module should define models that cross the client/server boundary
- `WEBAPP_ORIGIN` is a required runtime env var — it has a placeholder in `.env.example` but must be set correctly in any deployed environment
- The CI pipeline has three jobs with a strict `needs:` chain (`build → test → quality`) — if `build` fails, neither `test` nor `quality` runs
- The Vite dev server runs inside Docker Compose via a Node image and proxies `/api` — for local HMR development, running `./gradlew :app:webApp:jsBrowserDevelopmentRun` directly is faster and avoids the Docker overhead
