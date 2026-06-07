# Recurring Bug Patterns (`docs/memory/`)

For systemic, high-risk, or governance-level patterns, see `.specify/memory/security_constitution.md`.

---

### 2026-06-07 - B2: Kilua DOM event dispatch requires 80ms recomposition delay in component tests

**Status**: Active

**Pattern**
`element.dispatchEvent(mouseEvent())` followed by `delay(10.milliseconds)` → `shouldNotBeNull()`
fails because the new DOM nodes are absent. Compose state was updated but the recomposition pass
has not yet committed changes to the DOM.

**Root Cause**
Kilua/Compose recomposition is asynchronous. After a programmatic DOM event triggers a state
change, Compose schedules a recomposition pass. 10ms is not enough for that pass to complete.
`renderComponent` in `TestUtilsActual.kt` already uses `delay(80.milliseconds)` as the settle
time — the same budget is required after any event that triggers a state change.

**Fix**
Match `renderComponent`'s settle time after every `dispatchEvent()` call:

```kotlin
toggle.dispatchEvent(mouseEvent())
delay(80.milliseconds)          // ← not 10ms
host.querySelector("[role='listbox']").shouldNotBeNull()
```

**Prevention**
Any test that dispatches a DOM event and then queries the resulting DOM must wait at least 80ms.
If `renderComponent` ever changes its settle constant, update all post-event delays to match.

**Evidence**: `LanguageSwitcherDomTest` — "dropdown opens when toggle is clicked" failed with
`Expected value to not be null, but was null` using `delay(10.milliseconds)`.

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
