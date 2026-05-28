---
document_type: security-review
review_type: followup
assessment_date: 2026-05-28
codebase_analyzed: KodEx / code-cache
total_files_analyzed: 3
total_findings: 5
overall_risk: MODERATE
critical_count: 0
high_count: 0
medium_count: 1
low_count: 2
informational_count: 2
owasp_categories: [A03, A05, A06]
cwe_ids: [CWE-20, CWE-74, CWE-693, CWE-494]
field_summaries:
  document_type: "Always 'security-review'. Allows indexers to skip non-review documents."
  review_type: "Which command generated this document: audit, branch, staged, plan, tasks, or followup."
  assessment_date: "ISO 8601 date the review was performed (YYYY-MM-DD)."
  overall_risk: "Highest severity tier with active findings (CRITICAL, HIGH, MODERATE, LOW, INFORMATIONAL)."
  critical_count: "Number of Critical findings (CVSS 9.0-10.0)."
  high_count: "Number of High findings (CVSS 7.0-8.9)."
  medium_count: "Number of Medium findings (CVSS 4.0-6.9)."
  low_count: "Number of Low findings (CVSS 0.1-3.9)."
  informational_count: "Number of Informational findings."
  owasp_categories: "OWASP Top 10 2021 categories that have at least one finding."
  cwe_ids: "CWE identifiers referenced in this document."
  finding_id: "Unique finding identifier (SEC-NNN) for cross-referencing and task linkage."
  location: "File path and line number of the vulnerable code (path/to/file.ext:line)."
  owasp_category: "OWASP Top 10 category for this finding."
  cwe: "Common Weakness Enumeration identifier with short name."
  cvss_score: "CVSS v3.1 base score (0.0-10.0). 9.0+=Critical, 7.0-8.9=High, 4.0-6.9=Medium, 0.1-3.9=Low."
  spec_kit_task: "Spec-Kit task ID for backlog tracking and remediation follow-up (TASK-SEC-NNN)."
---

# Security Follow-Up Plan — `feature/002-design-system`

## Executive Summary

5 findings from the branch review were triaged against `specs/002-design-system/tasks.md`.
None of the findings are already covered by existing tasks. 3 findings are scheduled for
**immediate remediation** (before merge into `develop`): the missing CSP in `index.html`, the
unvalidated locale code, and the unguarded `NavItem.icon` class injection. 2 informational
findings are deferred as technical debt with explicit revisit triggers.

---

## Inputs Reviewed

| Artifact | Path |
|---|---|
| Branch security review | `docs/security-reviews/2026-05-28-feature-002-design-system-branch.md` |
| Active task backlog | `specs/002-design-system/tasks.md` |
| Current pending tasks | T031, T032, T042, T043, T044, T045, T046 |

---

## Resolution Decisions

| Finding | Severity | Decision | Rationale |
|---|---|---|---|
| SEC-001 — Missing CSP | Medium | **Implement now** | Low effort; fixes before webapp reaches production |
| SEC-002 — Locale not validated | Low | **Implement now** | One-liner; closes a post-XSS persistence vector |
| SEC-003 — `NavItem.icon` class injection | Low | **Implement now** | Add before NavItems are wired to API responses |
| SEC-004 — CI actions tag-pinned | Info | **Technical debt** | Industry-standard risk; not blocking this branch |
| SEC-005 — PNG glob too broad | Info | **Technical debt** | Cosmetic CI issue; no security impact |

---

## Immediate Remediation Tasks

### TASK-SEC-001 — Add Content Security Policy to `index.html` and Ktor response headers

| Field | Value |
|---|---|
| **Task ID** | TASK-SEC-001 |
| **Severity** | Medium |
| **OWASP** | A05:2021 — Security Misconfiguration |
| **CWE** | CWE-693 |
| **Source Finding** | SEC-001 |
| **Depends On** | — |
| **Phase** | Phase 8 (Polish) — add after T045 |

**What to do:**

1. Add CSP + Referrer-Policy meta tags to `index.html` (both `webMain` and `wasmJsMain`):

```html
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none';">
<meta name="referrer" content="strict-origin-when-cross-origin">
```

2. Add response security headers to the Ktor server in
   `server/app/src/main/kotlin/dev/kodex/server/Application.kt` (inside the `install(Headers)`
   block that already manages `Cache-Control`):

```kotlin
append(HttpHeaders.XContentTypeOptions, "nosniff")
append(HttpHeaders.XFrameOptions, "DENY")
append("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
append("Content-Security-Policy",
    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
    "font-src 'self' data:; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none';"
)
```

**Acceptance Criteria:**
- [ ] Both `index.html` files contain the CSP meta tag
- [ ] DevTools → Network → index.html response shows `Content-Security-Policy` header
- [ ] Browser console shows no CSP violation warnings on app load
- [ ] `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` present in API responses

---

### TASK-SEC-002 — Validate locale code from `localStorage` against allowlist

| Field | Value |
|---|---|
| **Task ID** | TASK-SEC-002 |
| **Severity** | Low |
| **OWASP** | A03:2021 — Injection |
| **CWE** | CWE-20 |
| **Source Finding** | SEC-002 |
| **Depends On** | — |
| **File** | `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/i18n/I18nSetup.kt` |

**What to do:**

Replace the unvalidated locale read in `initI18n()` and add a guard to `setLocale()`:

```kotlin
private val ALLOWED_LOCALES = setOf("en", "fa")

suspend fun initI18n() {
    if (!isDom) return
    val client = HttpClient()
    val enContent = client.get("/modules/i18n/messages-en.po").bodyAsText()
    val faContent = client.get("/modules/i18n/messages-fa.po").bodyAsText()
    client.close()
    i18n = I18n(
        "en" to enContent.asLocaleData(),
        "fa" to faContent.asLocaleData(),
    )
    val savedLocale = localStorage.getItem("kodex-locale")
        ?.takeIf { it in ALLOWED_LOCALES } ?: "en"  // ← validated
    LocaleManager.setCurrentLocale(SimpleLocale(language = savedLocale))
}

fun setLocale(code: String) {
    require(code in ALLOWED_LOCALES) { "Unsupported locale: $code" }
    localStorage.setItem("kodex-locale", code)
    LocaleManager.setCurrentLocale(SimpleLocale(language = code))
}
```

**Acceptance Criteria:**
- [ ] `ALLOWED_LOCALES` constant defined
- [ ] `initI18n()` uses `takeIf { it in ALLOWED_LOCALES }` before passing to `SimpleLocale`
- [ ] `setLocale()` has `require(code in ALLOWED_LOCALES)` guard
- [ ] Existing `setLocale("en")` and `setLocale("fa")` calls still work (no regression)

---

### TASK-SEC-003 — Add character validation to `NavItem.icon` field

| Field | Value |
|---|---|
| **Task ID** | TASK-SEC-003 |
| **Severity** | Low |
| **OWASP** | A03:2021 — Injection |
| **CWE** | CWE-74 |
| **Source Finding** | SEC-003 |
| **Depends On** | — |
| **File** | `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/components/NavBar.kt` |

**What to do:**

Add an `init` block to the `NavItem` data class:

```kotlin
data class NavItem(
    val key: String,
    val label: String,
    val icon: String? = null,
) {
    init {
        require(key.matches(Regex("[a-zA-Z0-9_\\-]+"))) {
            "NavItem.key must be alphanumeric/dash/underscore only"
        }
        require(icon == null || icon.matches(Regex("[a-zA-Z0-9_\\-: ]+"))) {
            "NavItem.icon must be a valid CSS class string"
        }
    }
}
```

Note: also validates `key` since it is used in DOM element IDs
(`id="navbar-item-${item.key}"`).

**Acceptance Criteria:**
- [ ] `NavItem` init block rejects icons with `<`, `>`, `"`, `'`, `;` characters
- [ ] All existing `NavItem(...)` usages in preview/playground files still compile and pass at runtime
- [ ] Unit test added: `NavItem(key="home", label="Home", icon="fa-house")` → valid; `NavItem(..., icon="fa-house\"><script>")` → throws `IllegalArgumentException`

---

## Technical Debt Backlog

### TASK-SEC-004 — Pin GitHub Actions to immutable commit SHAs

| Field | Value |
|---|---|
| **Task ID** | TASK-SEC-004 |
| **Type** | Technical Debt |
| **Severity** | Informational |
| **OWASP** | A06:2021 — Supply Chain |
| **Source Finding** | SEC-004 |
| **File** | `.github/workflows/ci.yml` |

**Why safe to defer:** Mutable tag pinning is industry-standard for GitHub Actions today. The
actions referenced (`dorny/paths-filter`, `actions/*`) are well-maintained and widely audited.
Risk is low for an internal project at this stage.

**Remaining risk:** A compromised or force-pushed action tag could execute arbitrary code in CI,
accessing any secrets available to the workflow (e.g., `GITHUB_TOKEN`).

**Revisit trigger:** Before the project enters beta / public access, or whenever a CI security
policy is formally adopted.

**What to do when revisited:**
```bash
# Use a tool like Dependabot or pin-github-actions to auto-generate SHA pins:
npx pin-github-actions .github/workflows/ci.yml
```

---

### TASK-SEC-005 — Narrow `**/*.png` glob in CI screenshot diff upload

| Field | Value |
|---|---|
| **Task ID** | TASK-SEC-005 |
| **Type** | Technical Debt |
| **Severity** | Informational |
| **Source Finding** | SEC-005 |
| **File** | `.github/workflows/ci.yml:174` |

**Why safe to defer:** No security impact. Artifact storage is private to the GitHub repo.
This is a cleanup item only.

**Remaining risk:** Slightly inflated CI artifact size; favicon PNGs may appear in test reports.

**Revisit trigger:** Next time `ci.yml` is touched for any reason.

**Fix (one line):**
```yaml
path: |
  test-results/
  app/webApp/src/webTest/ts/screenshot/**/*-diff.png
```

---

## Already Covered Items

None — no existing tasks in `tasks.md` (T001–T046) overlap with these security findings.

---

## Backlog-Ready Task Table

| Task ID | Title | Severity | Type | Source | Depends On | Acceptance Criteria |
|---------|-------|----------|------|--------|------------|---------------------|
| TASK-SEC-001 | Add CSP to `index.html` + Ktor security headers | Medium | Implement now | SEC-001 | — | CSP header present; no console violations on load |
| TASK-SEC-002 | Validate locale code from `localStorage` against allowlist | Low | Implement now | SEC-002 | — | `takeIf { it in ALLOWED_LOCALES }` in `initI18n()`; `require` in `setLocale()` |
| TASK-SEC-003 | Add character validation to `NavItem.icon` and `NavItem.key` | Low | Implement now | SEC-003 | — | Init block rejects invalid class chars; existing usages unchanged |
| TASK-SEC-004 | Pin CI actions to SHA | Info | Technical Debt | SEC-004 | — | Revisit before beta; use pin-github-actions tool |
| TASK-SEC-005 | Narrow PNG glob in CI artifact upload | Info | Technical Debt | SEC-005 | — | Revisit next time `ci.yml` is modified |

---

## Confirmed Secure Patterns (carried forward)

| Pattern | Location | Property |
|---------|----------|----------|
| Kilua `+` operator → text node only | All components | XSS-safe rendering |
| `EnvConfig.env()` hard-fails on missing secret | `core/.../EnvConfig.kt` | No insecure defaults |
| `npm ci` in CI | `.github/workflows/ci.yml` | Reproducible JS supply chain |
| `html2canvas` dev-only | `package.json devDependencies` | No production SSRF risk |
