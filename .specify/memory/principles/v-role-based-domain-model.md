# §V — Role-Based Domain Model

The system recognizes exactly two user roles:

- **Admin**: Can create, update, and publish exams; define exam mode, test cases, time limits,
  and scoring rules; view all submissions and results.
- **Participant**: Can browse and join published exams; submit solutions; view their own
  submission history and scores. MUST NOT see other participants' submissions or injected tests.

Role enforcement MUST happen at the backend API layer, not only in the UI. Every API endpoint
MUST declare which roles may access it. Unauthenticated requests to protected endpoints MUST
return HTTP 401; insufficient-role requests MUST return HTTP 403.

## Exam State Machine

Every exam MUST be in exactly one of three states:

| State | Who can see | Participant can submit | Admin can edit |
|---|---|---|---|
| `DRAFT` | Admin only | No | Yes (full edit) |
| `PUBLISHED` | All authenticated users | Yes | **No** |
| `CLOSED` | All authenticated users | No | No |

Transition rules:
- `DRAFT → PUBLISHED`: admin explicit action. Irreversible without closing first.
- `PUBLISHED → CLOSED`: admin action OR automatic when a configured deadline passes.
- `CLOSED` is terminal — no transitions out. A corrected exam requires creating a new exam.
- Modifying test cases, time limits, or questions in `PUBLISHED` state is **forbidden**,
  even for admins. This protects fairness for participants who already submitted.
- Result visibility to participants is configured by admin and takes effect only after `CLOSED`.

**Rationale**: Confusing admin and participant access would compromise exam integrity (leaking
test cases) and participant privacy (exposing others' submissions).
