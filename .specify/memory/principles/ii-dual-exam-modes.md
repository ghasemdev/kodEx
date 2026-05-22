# §II — Dual Exam Modes

The system MUST support exactly two exam modes:

- **Kotlin Mode**: The participant receives either (a) an input/output specification and writes
  a solution, or (b) a Kotlin file with a `main` function that they must complete. Grading is
  done by comparing stdout against expected output, or by running injected unit tests.

  An examiner MAY define test cases as **static** (fixed at exam-creation time) or **dynamic**
  (generated at evaluation time via a server-side generator function or parameterised template).
  Both variants are hidden from participants. A single exam MAY mix static and dynamic test
  cases. Dynamic test cases MUST be reproducible: the generator MUST accept a deterministic
  seed so the same inputs can be regenerated for dispute resolution.

- **Android Mode**: The participant receives a source-code scaffold (an incomplete Android
  project) with clearly marked `TODO` sections. They fill in the implementation. Grading is
  done via injected instrumented or unit tests that exercise the completed scaffold.

Every exam object MUST declare its `mode` field. No hybrid or mixed-mode exams are allowed in
v1. Adding a new mode requires a constitution amendment.

**Rationale**: Keeping two discrete, well-defined modes ensures the evaluation pipeline stays
focused and the sandbox infrastructure remains auditable and predictable. Dynamic test cases
allow examiners to prevent hard-coded solutions and to vary difficulty across exam sessions
without maintaining large banks of manually authored I/O pairs.
