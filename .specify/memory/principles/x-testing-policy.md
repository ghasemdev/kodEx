# §X — Testing Policy

Each module MUST have tests at the appropriate layer. Tests MUST NOT cross layer boundaries
via mocks — integration tests use real infrastructure.

| Module | Test type | Infrastructure |
|---|---|---|
| `server:domain` | Unit tests | None — pure Kotlin, zero I/O |
| `server:data` | Integration tests | Real PostgreSQL + Redis via [**Testcontainers**](https://github.com/testcontainers/testcontainers-java) |
| `server:api` | Integration tests | **Ktor test engine** + [Testcontainers](https://github.com/testcontainers/testcontainers-java) |
| `sandbox-runner` | Integration tests | Real Docker daemon (CI must have Docker) |
| `app:webApp` | Unit tests (MVI logic) | None; Kilua component tests if library supports |

Test framework: [**Kotest**](https://github.com/kotest/kotest) (Kotlin-native) for all JVM modules.
`kotlin.test` for `app:shared`.
Repository interfaces in `:domain` MUST NOT be mocked in `:data` or `:api` tests — use the
real implementation against the Testcontainer instance.

**Rationale**: Mocking repository interfaces in integration tests has historically hidden
migration and query bugs that only surface in production. Testcontainers cost is low;
mock/prod divergence cost is high.
