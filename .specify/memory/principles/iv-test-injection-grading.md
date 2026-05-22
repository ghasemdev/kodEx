# §IV — Test-Injection Grading

Grading MUST be performed server-side. The backend injects test cases into the user's
submission before it enters the sandbox. The participant MUST NOT see the injected tests.

For Kotlin Mode:
  - **Static test cases**: pre-authored I/O pairs (stdin → expected stdout) or hidden unit
    tests stored in the exam record. Injected verbatim at grading time.
  - **Dynamic test cases**: generated immediately before sandbox launch by a server-side
    generator. The generator receives the exam-defined seed and returns a list of I/O pairs
    or a Kotlin test file. The generated inputs/outputs MUST be persisted alongside the
    submission record for auditability (see §VI).
For Android Mode: hidden instrumented/unit tests are merged with the scaffold before building.

The sandbox returns pass/fail/error results per test case to the backend. Scoring logic
(partial credit, weights, penalty) runs exclusively in the backend — never in the sandbox.

**Rationale**: Client-side or sandbox-side scoring is trivially bypassable. Keeping evaluation
logic in trusted server code ensures result integrity. Persisting generated test inputs
guarantees that disputes can be re-evaluated against the exact inputs the participant faced.
