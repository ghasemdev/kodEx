# §VI — Auditability & Observability

Every submission event — received, sandbox started, sandbox finished, scored — MUST be logged
with a correlation ID, timestamp, user ID, exam ID, and outcome.
Sandbox resource usage (CPU time, peak memory, exit code) MUST be captured per execution.
Logs MUST be structured (JSON) so they are machine-parseable.
Deletion of submission records or audit logs is PROHIBITED in production.

**Rationale**: Disputes over grading, cheating investigations, and infrastructure debugging
all require a complete, tamper-evident audit trail.
