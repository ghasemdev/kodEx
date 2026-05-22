# §III — Secure Sandbox Execution

All user-submitted code MUST be executed inside an isolated Docker container.
The sandbox container MUST:
- Have no network access.
- Have a hard CPU and memory limit (configurable per exam, defaulting to 2 CPUs / 512 MB RAM).
- Have a hard wall-clock timeout (configurable per exam, defaulting to 10 seconds).
- Mount only the submission files and injected tests — no host filesystem access.
- Be destroyed immediately after execution completes (no container reuse).

The sandbox orchestrator MUST run as a **separate service** (`sandbox-runner`) — a standalone
Ktor process with its own container (the only component with Docker socket access).
The main backend API communicates with `sandbox-runner` over an authenticated HTTP channel
(shared secret header). Direct execution of user code in the main API process or on the host
machine is strictly forbidden.

**Rationale**: Isolating Docker socket access to a single dedicated service limits blast radius
if the orchestration layer is compromised. It also allows `sandbox-runner` to be scaled,
restarted, or replaced independently from the main API.
