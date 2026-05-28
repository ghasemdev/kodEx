---
document_type: security-review
review_type: whitebox-assessment
assessment_date: 2026-05-28
export_date: 2026-05-29
codebase_analyzed: KodEx / feature/002-design-system
total_files_analyzed: 18
total_findings: 5
residual_risk: LOW
critical_count: 0
high_count: 0
medium_count: 1
low_count: 2
informational_count: 2
findings_resolved: 3
findings_deferred: 2
owasp_categories: [A03, A05, A06]
cwe_ids: [CWE-20, CWE-74, CWE-494, CWE-693]
---

# KODEX — WHITEBOX SECURITY ASSESSMENT REPORT

**Feature Branch**: `feature/002-design-system`
**Assessment Date**: 2026-05-28
**Remediation Completed**: 2026-05-29
**Exported**: 2026-05-29
**Assessor**: AI Security Auditor (Spec-Kit Security Review Extension — Whitebox mode)

---

## 1. EXECUTIVE SUMMARY

### 1.1 Assessment Overview

This report consolidates the findings from a Whitebox Security Assessment conducted on
2026-05-28 against the `feature/002-design-system` branch of the KodEx platform, together
with full remediation evidence collected on 2026-05-29. The assessment was performed with
complete access to source code, architectural decision records, the KodEx Security Constitution
(`.specify/memory/security_constitution.md`), the prior feature-001 whitebox assessment, and
the durable repository memory hub (`docs/memory/`). The scope covered the Kilua/JS frontend
design system (13 UI components, i18n subsystem, Tailwind CSS integration, design token layer),
the screenshot testing pipeline additions to CI/CD, and all new JS/npm dependencies introduced
by the branch.

### 1.2 Risk Posture

**Overall Risk Rating at Assessment: MODERATE**
**Residual Risk Rating at Export: LOW**

| Severity      | Count | Status | Primary OWASP Categories |
|---------------|-------|--------|--------------------------|
| Critical      | 0     | —      | —                        |
| High          | 0     | —      | —                        |
| Medium        | 1     | ✅ Resolved | A05 — Security Misconfiguration |
| Low           | 2     | ✅ Resolved | A03 — Injection          |
| Informational | 2     | ⏳ Deferred | A06 — Supply Chain      |

All 3 actionable findings were remediated in commit `a7e4a9c2`. Two informational findings are
formally deferred as technical debt and tracked as `TASK-SEC-004` and `TASK-SEC-005`.

### 1.3 Key Findings and Strategic Impact

The most actionable finding was the **absence of a Content Security Policy** from the
`index.html` shell and Ktor response headers. Without a CSP, any future XSS vector — whether
from a Kotlin/JS compiler artifact, a compromised npm package in the dev pipeline, or a future
`innerHTML` misuse — would execute without browser restriction. This is classified as MEDIUM
because the current component set does not generate `innerHTML` (all content is rendered via
Kilua's `+` text-node operator), so the risk is pre-emptive rather than immediately exploitable.

The two LOW findings target the i18n subsystem and the `NavBar`/`Sidebar` components. Both
involve user- or API-controlled string values being consumed without an allowlist check —
the locale code from `localStorage` and the `icon` field of `NavItem`. At the current stage
these strings originate from trusted Kotlin call-sites, making exploitation theoretical.
However, both paths will become exploitation surfaces when the design system is wired to API
responses, which is the core motivation for fixing them pre-merge.

No backend, authentication, database, or secret-management code was introduced or modified in
a security-relevant way by this branch.

### 1.4 Remediation Roadmap

All 3 actionable findings were remediated in a single commit (`a7e4a9c2`,
`fix(security): NavItem and i18n validation, html policy, extract conventions`) before the
branch was ready for merge review. The two informational findings (CI action SHA pinning and
PNG artifact glob) are deferred with explicit revisit triggers and do not affect the branch
merge decision.

---

## 2. ASSESSMENT METHODOLOGY

### 2.1 Scope of Work

| Asset | Coverage |
|-------|----------|
| `app/webApp/src/webMain/resources/index.html` | HTML shell — CSP, meta headers — full review |
| `app/webApp/src/webMain/kotlin/.../design/i18n/I18nSetup.kt` | localStorage read, locale handling — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Button.kt` | Rendering, event handling — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Input.kt` | Form input, label-derived ID — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/TextArea.kt` | Form input — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Card.kt` | Content rendering — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Badge.kt` | Rendering — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/CodeBlock.kt` | Code rendering, clipboard API — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Modal.kt` | DOM event listeners, ARIA, focus trap — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/NavBar.kt` | Dynamic class construction, NavItem — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Sidebar.kt` | Dynamic class construction — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/Toast.kt` | Message display, auto-dismiss — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/LanguageSwitcher.kt` | Locale switching — full review |
| `app/webApp/src/webMain/kotlin/.../design/components/ThemeSwitcher.kt` | Theme switching — full review |
| `app/webApp/src/webMain/kotlin/.../design/theme/ThemeMode.kt` | Theme enum — full review |
| `app/webApp/package.json` / `package-lock.json` | JS dependency supply chain — full review |
| `.github/workflows/ci.yml` | Screenshot-test job, action pinning — full review |
| `gradle/libs.versions.toml` | New library versions (Kilua, TailwindCSS) — full review |

### 2.2 Testing Approach

This was a **Whitebox Security Assessment**. The assessor had full access to:

- Source code logic and internal data flows.
- The KodEx Security Constitution (version 1.0.0, ratified 2026-05-19), the authoritative
  contract for all security requirements.
- The prior feature-001 whitebox assessment
  (`docs/security-reviews/2026-05-23-feature-001-whitebox-assessment.md`) and its set of
  confirmed secure patterns, used as the baseline for carry-forward analysis.
- Architectural decision records and design intent in `docs/memory/` and `specs/`.

Findings were evaluated against the **OWASP Top 10 (2021)** and the **CWE/SANS** weakness
taxonomy. All findings were cross-referenced against the Security Constitution to determine
whether they represent a violation of an explicit rule or a defence-in-depth gap.

---

## 3. TECHNICAL FINDINGS

---

### SEC-001 — Missing Content Security Policy in HTML Shell

**Severity**: MEDIUM
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-693 — Protection Mechanism Failure
**CVSS v3.1**: 5.3 (AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N)
**Status**: ✅ Resolved — commit `a7e4a9c2`

#### 3.1.1 Description

The new `index.html` shell shipped with no Content Security Policy and no `Referrer-Policy`
meta tag. The Ktor server (`server/app/Application.kt`) already emitted
`X-Content-Type-Options`, `X-Frame-Options`, and `Referrer-Policy` response headers, but
these are set on API responses, not on the HTML document that boots the Kilua/JS application.
The browser loads `index.html` from Vite directly in development, not through the Ktor API,
so API-layer headers do not protect the frontend at all.

Without a CSP, any XSS injection vector — a compromised npm transitive package writing to
the DOM, a future `innerHTML` call in a third-party Kilua plugin, or a Kotlin/JS compiler
artifact — would execute without restriction. The current components use only Kilua's
text-node rendering (`+` operator), so there is no immediate injection surface; the finding
is pre-emptive hardening.

#### 3.1.2 Evidence (Vulnerable State)

**File**: `app/webApp/src/webMain/resources/index.html` (original `<head>`)

```html
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>KodEx</title>
  <!-- No CSP, no Referrer-Policy -->
</head>
```

#### 3.1.3 Exploit Scenario

1. A future `innerHTML` call or a compromised Kilua plugin introduces a DOM-write vector.
2. Attacker crafts a payload (e.g., via a stored XSS in exam content, once that feature
   exists).
3. Without a CSP, the payload executes with full origin privileges: exfiltrates auth tokens
   from `localStorage`, reads submission data, or makes authenticated API calls on the
   victim's behalf.

#### 3.1.4 Impact

Absence of a defense-in-depth layer against XSS. The browser provides no inline-script
blocking, no `frame-ancestors` protection, and no `connect-src` restriction. Severity is
MEDIUM rather than HIGH because the current codebase has no confirmed XSS vector.

#### 3.1.5 Remediation Applied

Two changes in commit `a7e4a9c2`:

**1. `app/webApp/src/webMain/resources/index.html`** — CSP and Referrer-Policy meta tags
added to both `jsMain` and `wasmJsMain` `index.html` files:

```html
<meta http-equiv="Content-Security-Policy"
      content="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';
               font-src 'self' data:; img-src 'self' data:; connect-src 'self';
               frame-ancestors 'none';">
<meta name="referrer" content="strict-origin-when-cross-origin">
```

Note: `'unsafe-inline'` for `style-src` is required by Tailwind v4's utility-class runtime.
All script sources remain `'self'`-only, prohibiting inline scripts and eval.

**2. `server/app/src/main/kotlin/dev/kodex/server/Application.kt`** — Security headers
consolidated in the Ktor `DefaultHeaders` plugin:

```kotlin
install(DefaultHeaders) {
    header("X-Content-Type-Options", "nosniff")
    header("X-Frame-Options", "DENY")
    header("Referrer-Policy", "strict-origin-when-cross-origin")
    header("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
    header(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
            "font-src 'self' data:; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none';",
    )
}
```

---

### SEC-002 — Locale Code from `localStorage` Not Validated Against Allowlist

**Severity**: LOW
**OWASP Category**: A03:2021 — Injection
**CWE**: CWE-20 — Improper Input Validation
**CVSS v3.1**: 3.1 (AV:N/AC:H/PR:N/UI:R/S:U/C:N/I:L/A:N)
**Status**: ✅ Resolved — commit `a7e4a9c2`

#### 3.2.1 Description

The i18n initialisation routine read the locale identifier from `localStorage` and passed it
directly to `LocaleManager.setCurrentLocale()` without checking whether the value is one of
the two supported locales (`en`, `fa`). The `setLocale()` helper accepted any `String code`
without validation.

An attacker who has already achieved any write capability on `localStorage` (e.g., via a
stored XSS in a future feature) could pre-write an arbitrary string to `kodex-locale` and
have it flow into `SimpleLocale`. While the Kilua i18n library performs only string key
lookups with the locale value and would silently fall back on a miss, the unvalidated path
violates the Security Constitution §5 input-validation requirement and establishes a
post-XSS persistence vector.

#### 3.2.2 Evidence (Vulnerable State)

**File**: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/i18n/I18nSetup.kt:29`

```kotlin
// VULNERABLE — any string from localStorage flows into SimpleLocale
val savedLocale = localStorage.getItem("kodex-locale") ?: "en"
LocaleManager.setCurrentLocale(SimpleLocale(language = savedLocale))
```

```kotlin
// VULNERABLE — no validation on the caller-supplied code
fun setLocale(code: String) {
    localStorage.setItem("kodex-locale", code)
    LocaleManager.setCurrentLocale(SimpleLocale(language = code))
}
```

#### 3.2.3 Exploit Scenario

1. Attacker achieves a write to `localStorage` (e.g., via stored XSS in future exam content).
2. Attacker writes `kodex-locale = "../../etc/passwd"` or a locale string crafted to trigger
   a path-traversal or lookup error in the i18n library.
3. On next page load the poisoned value flows into the locale engine.

#### 3.2.4 Impact

At current library versions the locale value is used only for translation key lookups,
limiting direct impact. However, the unvalidated path is a post-XSS amplification risk
and a violation of the Security Constitution's allowlist-over-denylist rule.

#### 3.2.5 Remediation Applied

Commit `a7e4a9c2` — `I18nSetup.kt`:

```kotlin
private val ALLOWED_LOCALES = setOf("en", "fa")

suspend fun initI18n() {
    // ...
    val savedLocale = localStorage.getItem("kodex-locale")
        ?.takeIf { it in ALLOWED_LOCALES } ?: "en"   // ← validated
    LocaleManager.setCurrentLocale(SimpleLocale(language = savedLocale))
}

fun setLocale(code: String) {
    require(code in ALLOWED_LOCALES) { "Unsupported locale: $code" }
    localStorage.setItem("kodex-locale", code)
    LocaleManager.setCurrentLocale(SimpleLocale(language = code))
}
```

---

### SEC-003 — `NavItem.icon` Injected Directly into CSS Class Attribute

**Severity**: LOW
**OWASP Category**: A03:2021 — Injection
**CWE**: CWE-74 — Improper Neutralization of Special Elements in Output
**CVSS v3.1**: 2.6 (AV:N/AC:H/PR:L/UI:R/S:U/C:N/I:L/A:N)
**Status**: ✅ Resolved — commit `a7e4a9c2`

#### 3.3.1 Description

The `icon` field of the `NavItem` data class was interpolated directly into a CSS class
string in both `NavBar.kt` and `Sidebar.kt`. The `NavItem` data class had no constructor
validation, and the `key` field (used to construct DOM element IDs) was similarly unchecked.

At assessment time all `NavItem` instances were constructed in statically-typed Kotlin
code, making direct exploitation impossible. However, the design system is the foundation
for all future screens. When `NavItem` lists are derived from API responses — menu
configurations, exam navigation, role-based sidebar items — the unguarded `icon` field
becomes an injection surface for any API caller with write access to nav definitions.

#### 3.3.2 Evidence (Vulnerable State)

**File**: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/components/NavBar.kt:53`

```kotlin
// NavBar.kt — VULNERABLE: icon value not validated
if (item.icon != null) span(className = "${item.icon} w-4 text-center") {}
```

**File**: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/design/components/Sidebar.kt:73`

```kotlin
// Sidebar.kt — VULNERABLE: same pattern
if (item.icon != null) span(className = "${item.icon} w-4 text-center flex-shrink-0") {}
```

#### 3.3.3 Exploit Scenario

1. API response populates a nav definition with `icon = "fa-home\" onmouseover=\"alert(1)"`.
2. The unvalidated string is placed directly into the `className` attribute.
3. Depending on the browser's HTML parsing of malformed class attributes, unexpected behavior
   may result. In Kilua's DOM model the class is set via `element.className`, limiting the
   direct XSS risk — but the injection breaks the CSS class semantics and lays groundwork
   for future exploitation.

#### 3.3.4 Impact

Class injection into DOM elements derived from API data. Severity constrained to LOW because
(a) there is no confirmed XSS escape from the `className` property in Kilua's current output,
and (b) API data routes are not yet implemented. The fix is pre-emptive.

#### 3.3.5 Remediation Applied

Commit `a7e4a9c2` — `NavBar.kt` `NavItem` data class:

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

Both the `key` field (used in DOM IDs) and the `icon` field are now validated at
construction. Existing call-sites in the playground previews all use conforming strings
and are unaffected.

---

### SEC-004 — CI Actions Pinned to Mutable Tags, Not Immutable SHAs

**Severity**: INFORMATIONAL
**OWASP Category**: A06:2021 — Vulnerable and Outdated Components (Supply Chain)
**CWE**: CWE-494 — Download of Code Without Integrity Check
**CVSS v3.1**: 0.0 (no current exploit path; defence-in-depth)
**Status**: ⏳ Deferred — TASK-SEC-004

#### 3.4.1 Description

The new `screenshot-test` job added by this branch uses `dorny/paths-filter@v3`, a
third-party GitHub Action pinned to a mutable tag. Tags in GitHub Actions can be
force-pushed to point to different commits at any time; tag pinning provides no integrity
guarantee. The existing first-party actions (`actions/checkout@v4`, `actions/setup-node@v4`,
`actions/upload-artifact@v4`) across all CI jobs are also tag-pinned.

#### 3.4.2 Evidence

**File**: `.github/workflows/ci.yml:144`

```yaml
- uses: dorny/paths-filter@v3       # ← mutable tag — no integrity guarantee
```

```yaml
- uses: actions/checkout@v4         # ← mutable tag
- uses: actions/setup-java@v4       # ← mutable tag
- uses: actions/setup-node@v4       # ← mutable tag
- uses: actions/upload-artifact@v4  # ← mutable tag
```

#### 3.4.3 Accepted Risk and Remediation Plan

**Why deferred**: Mutable tag pinning is the current industry default for GitHub Actions.
The actions in use are maintained by GitHub itself (`actions/*`) or by a widely audited
OSS project (`dorny/paths-filter`). Risk is low for an internal project.

**Remaining risk**: A compromised or force-pushed action tag could execute arbitrary code
in the CI environment, with access to all workflow secrets
(e.g., `GITHUB_TOKEN`, `PERSONAL_ACCESS_TOKEN`, `NVD_API_KEY`).

**Revisit trigger**: Before the repository is opened to external contributors or made public.

**Remediation plan**: Pin all actions to their full commit SHA using a tool such as
`npx pin-github-actions .github/workflows/ci.yml`. Example:

```yaml
# Before:
uses: dorny/paths-filter@v3
# After:
uses: dorny/paths-filter@de90cc6fb38fc0963ad72b210f1f284cd68cea36  # v3.0.2
```

---

### SEC-005 — Overly Broad `**/*.png` Glob in CI Artifact Upload

**Severity**: INFORMATIONAL
**OWASP Category**: A05:2021 — Security Misconfiguration (artifact hygiene)
**Status**: ⏳ Deferred — TASK-SEC-005

#### 3.5.1 Description

The screenshot diff upload step in the `screenshot-test` CI job uses the glob
`app/webApp/src/webTest/ts/screenshot/**/*-diff.png`, which is appropriate, but the step
also includes the blanket path `test-results/` without further scoping. More broadly, the
upload uses `if: failure()`, so artifacts are only produced on test failure — but the scope
of what is collected is wider than necessary for a diff review.

#### 3.5.2 Accepted Risk and Remediation Plan

**Why deferred**: CI artifacts are stored in private GitHub artifact storage, accessible
only to repository members. No security-sensitive data is captured. This is a cleanliness
finding only.

**Remaining risk**: Slightly inflated CI artifact size; committed assets (favicon PNGs)
may appear in test failure reports, causing minor confusion.

**Revisit trigger**: Next time `.github/workflows/ci.yml` is modified for any reason.

**Fix**:

```yaml
path: |
  test-results/
  app/webApp/src/webTest/ts/screenshot/**/*-diff.png
```

---

## 4. ARCHITECTURAL DRIFT AND SYSTEMIC RISKS

### 4.1 XSS Attack Surface is Currently Zero — Must Remain So

Every component in the design system renders content exclusively through Kilua's `+`
operator, which appends text nodes (i.e., calls `textContent`, never `innerHTML`). This is
the correct pattern and MUST be maintained as the design system is extended. The key risk
vectors to monitor in future components:

- Any use of `element.innerHTML` or `element.outerHTML`
- `js("...")` expressions that touch the DOM with unsanitized strings
- Third-party Kilua plugins that render user content

The CSP applied in SEC-001 is a defence-in-depth layer, not a substitute for this
text-node discipline. Both controls MUST remain in place.

### 4.2 `NavItem` Validation Establishes a Pattern for All Data-Bound Components

The `NavItem.init` block (SEC-003 remediation) sets the correct pattern for all future
design system components that will accept data from API responses: constructor-level
allowlist validation using Kotlin's `require()`. This pattern MUST be applied whenever a
component field is used:

- In a DOM attribute (`className`, `id`, `href`, `src`)
- In a template string that reaches the DOM
- As a key in a Map or lookup structure

The `LanguageSwitcher` component renders the locale code as a display label only (not as
a class or attribute), so it is not currently affected. If locale codes are ever used in
attribute positions, the `ALLOWED_LOCALES` set from SEC-002 MUST be applied there too.

### 4.3 Dev Playground Isolation Is Structurally Sound

The playground is gated by `js("import.meta.env.DEV")` in `App.kt` and resides in the
`webMain` source set. Vite's tree-shaking removes playground code from the production bundle
at build time. This is the correct isolation mechanism for a Kotlin/JS frontend that uses
a single-module Vite build pipeline. The `devMain` source-set pattern from `server:app`
does not apply here because WASM-JS and JS targets cannot use source-set conditional
compilation in the same way (see `specs/002-design-system/research.md`, Decision 6).

No security findings arose from the playground implementation. The guard is in place, the
mechanism is appropriate, and the production exclusion is structurally enforced.

### 4.4 Supply Chain: JS Dependencies Locked and Dev-Only

All JS packages introduced by this branch (`@fontsource/*`, `highlight.js`, `html2canvas`,
`playwright`) are locked via `package-lock.json`. `html2canvas` is correctly placed in
`devDependencies` and is absent from the production Vite bundle. The CI pipeline uses
`npm ci` (not `npm install`), ensuring the lockfile is always respected in automated
builds.

The one structural gap is that the webapp Docker Compose service runs `npm install` at
container startup rather than using a pre-built image. This means the Docker Compose
development environment reaches out to the npm registry on every restart. This is an
infrastructure finding tracked separately in `TASK-SEC-007` and `TASK-SEC-008`.

---

## 5. APPENDICES

### 5.1 Confirmed Secure Patterns (New, This Feature)

These patterns were validated in this assessment and MUST be maintained in all future
features that extend the design system.

| Pattern | Enforcement Location |
|---------|---------------------|
| Text-node rendering only — no `innerHTML` | All 13 design system components (Kilua `+` operator) |
| CSP meta tag in HTML shell | `app/webApp/src/webMain/resources/index.html` (both `jsMain` and `wasmJsMain`) |
| Locale code validated against allowlist before use | `app/webApp/.../design/i18n/I18nSetup.kt` — `ALLOWED_LOCALES` set |
| Component field validation in `init` block | `NavBar.kt` `NavItem` — `require()` for `key` and `icon` |
| Clipboard accessed via Web API only | `CodeBlock.kt` — `navigator.clipboard.writeText(code)` |
| Modal event listener cleaned up on unmount | `Modal.kt` — `DisposableEffect.onDispose` removes `keydown` listener |
| Playground guarded by `import.meta.env.DEV` | `App.kt` — Vite tree-shakes playground code from production bundle |
| `html2canvas` restricted to `devDependencies` | `package.json` — not bundled in production |
| JS dependencies locked via `package-lock.json` | `app/webApp/package-lock.json` — `npm ci` used in CI |

### 5.2 Confirmed Secure Patterns (Carry-Forward from Feature 001)

| Pattern | Enforcement Location |
|---------|---------------------|
| `env()` throws on missing secrets — no silent defaults | `core/env/EnvConfig.kt` |
| CORS restricted to exact `WEBAPP_ORIGIN` — no wildcard | `server/app/Application.kt` |
| Multi-stage Docker build — no build tools in production image | `docker/server.Dockerfile` |
| Non-root user in all containers | `docker/server.Dockerfile`, `docker/sandbox-runner.Dockerfile`, `sandbox/kotlin/Dockerfile` |
| `Cache-Control: no-store` on sensitive endpoints | `server/api/routes/HealthRoutes.kt` |
| Error responses never contain exception messages or stack traces | `server/app/Application.kt` |
| Redis requires password in production (`REDIS_PASSWORD` env var) | `docker/docker-compose.yml` |
| Full security header suite on all API responses | `server/app/Application.kt` |
| Secret scanning on every PR — full history (`fetch-depth: 0`) | `.github/workflows/ci.yml` |
| postgres/redis not bound to host ports in base compose | `docker/docker-compose.yml` |
| Shared secret required on all sandbox-runner routes except `/health` | `sandbox-runner/app/Application.kt` |
| sandbox-runner port not exposed to host | `docker/docker-compose.yml` |

### 5.3 CVSS Scoring Reference

| Score Range | Severity      |
|-------------|---------------|
| 9.0 – 10.0  | Critical      |
| 7.0 – 8.9   | High          |
| 4.0 – 6.9   | Medium        |
| 0.1 – 3.9   | Low           |
| 0.0         | Informational |

### 5.4 Finding Cross-Reference

| Finding | Severity | CVSS | OWASP | CWE | Status | Commit |
|---------|----------|------|-------|-----|--------|--------|
| SEC-001 — Missing CSP in HTML shell | MEDIUM | 5.3 | A05 | CWE-693 | ✅ Resolved | `a7e4a9c2` |
| SEC-002 — Locale code not validated | LOW | 3.1 | A03 | CWE-20 | ✅ Resolved | `a7e4a9c2` |
| SEC-003 — NavItem.icon class injection | LOW | 2.6 | A03 | CWE-74 | ✅ Resolved | `a7e4a9c2` |
| SEC-004 — CI actions tag-pinned | INFO | 0.0 | A06 | CWE-494 | ⏳ Deferred | TASK-SEC-004 |
| SEC-005 — PNG glob too broad | INFO | 0.0 | A05 | — | ⏳ Deferred | TASK-SEC-005 |

### 5.5 Tooling Context

- **Assessment Framework**: Spec-Kit Security Review Extension (Whitebox mode)
- **Memory Access**: Markdown-only (`.specify/memory/`, `docs/memory/`)
- **Prior Assessment**: `docs/security-reviews/2026-05-23-feature-001-whitebox-assessment.md`
- **Standards**: OWASP Top 10 (2021), CWE/SANS Top 25
- **Security Constitution**: `.specify/memory/security_constitution.md` v1.0.0
- **Branch Review**: `docs/security-reviews/2026-05-28-feature-002-design-system-branch.md`
- **Follow-Up Plan**: `docs/security-reviews/2026-05-28-feature-002-design-system-followup.md`
- **Initial Assessment Date**: 2026-05-28
- **Remediation Completed**: 2026-05-29 (commit `a7e4a9c2`)
- **Export Date**: 2026-05-29
