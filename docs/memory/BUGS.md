# Recurring Bug Patterns (`docs/memory/`)

For systemic, high-risk, or governance-level patterns, see `.specify/memory/security_constitution.md`.

---

### 2026-05-20 - B1: Wrong docker-java alias in version catalog

**Status**: Active

**Pattern**
Using `libs.docker.java` (direct alias) in a build file → Gradle configuration-time failure with an unhelpful "unresolved reference" error.

**Root Cause**
`docker-java` is registered in `libs.versions.toml` as a **bundle**, not a single alias:

```toml
[libraries]
docker-java-core = { module = "com.github.docker-java:docker-java-core", version.ref = "docker-java" }
docker-java-transport-httpclient5 = { module = "com.github.docker-java:docker-java-transport-httpclient5", version.ref = "docker-java" }

[bundles]
docker = ["docker-java-core", "docker-java-transport-httpclient5"]
```

**Fix**

```kotlin
// ✅ Correct
implementation(libs.bundles.docker)

// ❌ Wrong — alias does not exist
implementation(libs.docker.java)
```

**Prevention**
Before using a new catalog alias, check `libs.versions.toml` to confirm whether it is a single library entry or a bundle.
