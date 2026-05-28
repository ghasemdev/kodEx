---
document_type: security-review
review_type: branch
assessment_date: 2026-05-28
codebase_analyzed: KodEx / code-cache
total_files_analyzed: 18
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

# SECURITY REVIEW REPORT — BRANCH: `feature/002-design-system` vs `develop`

## Executive Summary

The `feature/002-design-system` branch introduces the KodEx frontend design system: 12 UI
components (Button, Input, Modal, CodeBlock, Toast, NavBar, Sidebar, etc.), an i18n subsystem,
Tailwind CSS integration, a screenshot testing pipeline, and toolchain additions. No backend
logic was modified.

The branch is **low-risk overall**. All text rendering uses Kilua's `+` operator (safe text
nodes, never `innerHTML`), so XSS via component content is not possible. The backend
secret-management and auth code touched in this branch are cosmetic renames only. The most
actionable finding is the **missing Content Security Policy** in `index.html`, which should be
addressed before the webapp goes to production. Two low-severity issues in i18n and icon-class
handling should be fixed before the design system is wired to dynamic API data.

---

## Branch Diff Reviewed

| | |
|---|---|
| **Target** | `feature/002-design-system` |
| **Base** | `develop` |

**Security-relevant files analyzed:**

| File | Reason |
|---|---|
| `app/webApp/src/webMain/resources/index.html` | HTML shell — CSP, meta headers |
| `app/webApp/src/webMain/kotlin/.../design/i18n/I18nSetup.kt` | localStorage read, locale handling |
| `app/webApp/src/webMain/kotlin/.../design/components/CodeBlock.kt` | Code content rendering, clipboard |
| `app/webApp/src/webMain/kotlin/.../design/components/Modal.kt` | DOM event listeners, ARIA |
| `app/webApp/src/webMain/kotlin/.../design/components/NavBar.kt` | Dynamic class construction |
| `app/webApp/src/webMain/kotlin/.../design/components/Sidebar.kt` | Dynamic class construction |
| `app/webApp/src/webMain/kotlin/.../design/components/Toast.kt` | Message display |
| `app/webApp/src/webMain/kotlin/.../design/components/Input.kt` | Form input, label-derived ID |
| `app/webApp/src/webMain/kotlin/.../design/theme/ThemeMode.kt` | Theme enum, no I/O |
| `core/src/main/kotlin/dev/kodex/core/env/EnvConfig.kt` | Secret loading |
| `server/app/src/main/kotlin/dev/kodex/server/Application.kt` | Koin wiring |
| `server/app/src/main/kotlin/dev/kodex/server/ServerModule.kt` | Module definition |
| `server/app/src/dev/kotlin/.../dev/DevModule.kt` | Dev-only module |
| `.github/workflows/ci.yml` | CI pipeline, supply chain |
| `gradle/libs.versions.toml` | Dependency versions |
| `package.json` / `package-lock.json` | JS dependency lockfile |

---

## Vulnerability Findings

### [MEDIUM] Missing Content Security Policy in HTML shell

**Finding ID:** SEC-001
**Location:** `app/webApp/src/webMain/resources/index.html` (entire file)
**CVSS Score:** 5.3 (AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N)
**OWASP Category:** A05:2021 — Security Misconfiguration
**CWE:** CWE-693 — Protection Mechanism Failure

**Description:**
The new `index.html` ships with no Content Security Policy (CSP). There is no
`<meta http-equiv="Content-Security-Policy">` tag, and no mention of CSP headers in the Ktor
server configuration for this branch. Without CSP, any successful XSS injection (e.g., from a
future `innerHTML` misuse, a compromised CDN resource, or a Kotlin/JS compiler bug) can execute
without browser restriction.

Additionally, the following security-relevant headers/meta tags are absent:
- `Referrer-Policy` — controls how much URL is sent in `Referer` headers
- `X-Content-Type-Options: nosniff` — must be set as an HTTP response header
- `Permissions-Policy` — restricts browser features

**Remediation:**
Add a CSP meta tag as a first-layer defense. The full policy should be enforced via HTTP
response headers in `server/app/src/main/kotlin/dev/kodex/server/Application.kt` (the existing
Ktor `Headers` plugin location). A starter policy appropriate for this app:

```html
<!-- index.html <head> -->
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none';">
<meta name="referrer" content="strict-origin-when-cross-origin">
```

Note: `'unsafe-inline'` for styles is required by Tailwind's utility-class approach but all
scripts must remain `'self'`-only.

**Spec-Kit Task:** TASK-SEC-001

---

### [LOW] Locale Code from `localStorage` Not Validated Against Allowlist

**Finding ID:** SEC-002
**Location:** `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/i18n/I18nSetup.kt:29`
**CVSS Score:** 3.1 (AV:N/AC:H/PR:N/UI:R/S:U/C:N/I:L/A:N)
**OWASP Category:** A03:2021 — Injection
**CWE:** CWE-20 — Improper Input Validation

**Description:**
The locale code is read directly from `localStorage` without validation:

```kotlin
val savedLocale = localStorage.getItem("kodex-locale") ?: "en"
LocaleManager.setCurrentLocale(SimpleLocale(language = savedLocale))
```

If any XSS vector exists (now or in the future), an attacker could pre-write an arbitrary string
to `kodex-locale` in `localStorage` and have it passed into `SimpleLocale`. While the current
Kilua i18n library likely does benign string lookups with the locale value, the pattern is a
post-XSS persistence vector and violates the security constitution's input validation requirement.

Similarly, the `setLocale` function in the same file takes any `String code` without validation.

**Remediation:**

```kotlin
private val ALLOWED_LOCALES = setOf("en", "fa")

suspend fun initI18n() {
    // ...
    val savedLocale = localStorage.getItem("kodex-locale")
        ?.takeIf { it in ALLOWED_LOCALES } ?: "en"
    // ...
}

fun setLocale(code: String) {
    require(code in ALLOWED_LOCALES) { "Unsupported locale: $code" }
    localStorage.setItem("kodex-locale", code)
    LocaleManager.setCurrentLocale(SimpleLocale(language = code))
}
```

**Spec-Kit Task:** TASK-SEC-002

---

### [LOW] `NavItem.icon` Injected Directly into CSS Class Attribute

**Finding ID:** SEC-003
**Location:** `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/components/NavBar.kt:53`,
`app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/components/Sidebar.kt:73`
**CVSS Score:** 2.6 (AV:N/AC:H/PR:L/UI:R/S:U/C:N/I:L/A:N)
**OWASP Category:** A03:2021 — Injection
**CWE:** CWE-74 — Improper Neutralization of Special Elements in Output

**Description:**
The `icon` field of `NavItem` is interpolated directly into a CSS `className` string:

```kotlin
// NavBar.kt:53
if (item.icon != null) span(className = "${item.icon} w-4 text-center") {}

// Sidebar.kt:73
if (item.icon != null) span(className = "${item.icon} w-4 text-center flex-shrink-0") {}
```

Currently `NavItem` instances are constructed in application Kotlin code, so the risk is
limited. However, when this design system is integrated with the real app, `NavItem` lists will
likely be derived from API responses (menu definitions, exam navigation). If the `icon` field is
populated from API data without sanitization, an attacker with write access to exam/nav data
could inject unexpected CSS class names.

**Remediation:**

```kotlin
data class NavItem(
    val key: String,
    val label: String,
    val icon: String? = null,
) {
    init {
        require(icon == null || icon.matches(Regex("[a-zA-Z0-9_\\-: ]+"))) {
            "NavItem.icon contains invalid characters"
        }
    }
}
```

**Spec-Kit Task:** TASK-SEC-003

---

### [INFORMATIONAL] CI Actions Pinned to Mutable Tags, Not Immutable SHAs

**Finding ID:** SEC-004
**Location:** `.github/workflows/ci.yml:139,145,149,155,160,166`
**OWASP Category:** A06:2021 — Vulnerable and Outdated Components (Supply Chain)
**CWE:** CWE-494 — Download of Code Without Integrity Check

**Description:**
The new `screenshot-test` job uses `dorny/paths-filter@v3` — a third-party action pinned to a
mutable tag. Tags in GitHub Actions can be force-pushed to point to different commits. The
existing first-party actions (`actions/checkout@v4`, `actions/setup-node@v4`,
`actions/upload-artifact@v4`) are also tag-pinned across the workflow.

**Remediation:**
Pin all actions to their full commit SHA. Example:

```yaml
# Instead of:
uses: dorny/paths-filter@v3
# Use:
uses: dorny/paths-filter@de90cc6fb38fc0963ad72b210f1f284cd68cea36  # v3.0.2
```

**Spec-Kit Task:** TASK-SEC-004

---

### [INFORMATIONAL] Overly Broad `**/*.png` Glob in CI Artifact Upload

**Finding ID:** SEC-005
**Location:** `.github/workflows/ci.yml:174`
**OWASP Category:** A05:2021 — Security Misconfiguration

**Description:**
The "Upload screenshot diffs" step uses `**/*.png` which captures all PNG files in the entire
workspace — including committed favicon and branding assets. This doesn't create a security
vulnerability but bloats CI artifacts and could unintentionally expose binary assets in CI
artifact storage.

**Remediation:**

```yaml
path: |
  test-results/
  app/webApp/src/webTest/ts/screenshot/**/*-diff.png
```

**Spec-Kit Task:** TASK-SEC-005

---

## Confirmed Secure Patterns

| Pattern | Evidence | Security Property |
|---|---|---|
| Text-node rendering only | All components use Kilua `+` operator (textContent, not innerHTML) | XSS-safe content rendering |
| Hard-fail on missing secrets | `EnvConfig.kt` throws `error("Missing required env var: $key")` — no defaults | Secrets constitution compliant |
| Clipboard API usage | `navigator.clipboard.writeText(code)` — standard Web API | No custom clipboard injection |
| Modal event listener cleanup | `DisposableEffect.onDispose` removes `keydown` listener | No listener memory leak |
| Reproducible JS builds | CI uses `npm ci` (locked from `package-lock.json`) | Supply chain integrity for JS |
| `html2canvas` is dev-only | Present only in `devDependencies`, not in production bundle | No production SSRF risk |
| Backend changes are cosmetic | `EnvConfig`, `ServerModule`, `DevModule` — only SCREAMING_SNAKE_CASE renames | No change to auth/CORS/RBAC posture |
| `type="module"` script | `index.html` loads app bundle as ES module | Deferred, cross-origin isolation-safe |

---

## Prioritized Action Plan

| Priority | Finding | Effort | Before Merge? |
|---|---|---|---|
| 1 | SEC-001 — Add CSP to `index.html` + Ktor headers | Low | Recommended |
| 2 | SEC-002 — Validate locale code against allowlist | Low | Yes |
| 3 | SEC-003 — Validate/type `NavItem.icon` | Low | Recommended |
| 4 | SEC-004 — Pin CI actions to SHA | Medium | No (backlog) |
| 5 | SEC-005 — Narrow PNG artifact glob | Low | No (backlog) |
