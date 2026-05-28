# Implementation Plan: Client-Side Design System

**Branch**: `feature/002-design-system` | **Date**: 2026-05-23 | **Spec**: [spec.md](spec.md)

## Summary

Build a complete design system for `app/webApp` using Kilua 0.0.34 + TailwindCSS v4.
Deliverables: Kotlin-inspired design token palette (light/dark via `ThemeManager`), three
self-hosted fonts (Inter / Vazirmatn / JetBrains Mono), English + Persian i18n with RTL layout
(`kilua-i18n` + `kotlinx-gettext`), 13 reusable components (all Tailwind-styled), fully
responsive layout across four viewport tiers (Mobile / Tablet / Desktop / TV), and a
dev-only component playground (excluded from production via Vite tree-shaking).

## Technical Context

**Language/Version**: Kotlin **2.3.21** (JS + WASM-JS targets); Node.js **22+**

**Primary Dependencies**:

- UI framework: Kilua **0.0.34** (`dev.kilua:kilua`, `kilua-tailwindcss`, `kilua-i18n`, `kilua-fontawesome`)
- CSS: TailwindCSS **4.0.13** (npm `tailwindcss`, Vite plugin `@tailwindcss/vite`)
- i18n: `kilua-i18n:0.0.34` + `kotlinx-gettext` Gradle plugin **0.7.0**
- Fonts: `@fontsource/inter:5.1.1`, `@fontsource/vazirmatn:5.1.1`, `@fontsource/jetbrains-mono:5.1.1`
- Code highlighting: `highlight.js:11.9.1`
- Bundler: Vite **8** (already configured in `app/webApp/vite.config.ts`)

**Storage**: No server-side storage. Theme + locale preferences persisted to browser `LocalStorage`
via Kilua's `ThemeManager` and `LocaleManager` (already handle persistence internally).

**Testing**: `kotlin.test` (existing, via `kotlin-kmp-convention`). Visual regression via Dev
Playground manual breakpoint testing. No new test frameworks required.

**Target Platform**: Kotlin/JS + WASM-JS browser targets (existing dual-target setup preserved).

**Project Type**: Frontend component library within a KMP monorepo. Changes are scoped
entirely to `app/webApp` and `gradle/libs.versions.toml`. No backend or `core:models` changes.

**Performance Goals**:
- PG-001: Production CSS bundle size ≤ 20 KB gzipped (Tailwind v4 purges unused classes)
- PG-002: Font load: FOUT ≤ 100 ms (preloaded via `<link rel=preload>` in `index.html`)
- PG-003: Theme switch and locale switch complete within one render frame (no page reload)

**Constraints**:
- A1: Type-safe project accessors mandatory — `project(":x")` prohibited
- A2: Dev playground excluded from production artifact (via Vite `import.meta.env.DEV` tree-shaking)
- A3: Convention plugin composition — Tailwind `vite { }` config and i18n plugin go directly
  in `app/webApp/build.gradle.kts` (module-specific, not quality tooling; A3-compliant)
- All secrets remain via environment variables — no new secrets introduced by this feature
- `devMain` source set pattern (server:app) is not applicable to JS/WASM targets; Vite
  tree-shaking achieves the equivalent isolation (see research.md Decision 6)

**Scale/Scope**: 13 components, 2 locales, 4 breakpoint tiers, 1 frontend module.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Applicable | Status | Notes |
|---|---|---|---|
| I — Kotlin-First Stack | ✅ Yes | ✅ PASS | All component code in Kotlin; `kilua-i18n` is Kotlin; font/tailwind tooling is build-time only |
| II — Dual Exam Modes | ➖ N/A | — | No exam logic in design system |
| III — Secure Sandbox | ➖ N/A | — | No sandbox interaction |
| IV — Test-Injection Grading | ➖ N/A | — | No grading logic |
| V — Role-Based Domain Model | ➖ N/A | — | No role gating on design system components |
| VI — Auditability & Observability | ➖ N/A | — | No submission or audit events |
| VII — Architecture & Module Conventions | ✅ Yes | ✅ PASS | MVI preserved; `webApp` remains the only UI module; convention plugins respected (A3); type-safe accessors (A1); dev isolation via Vite tree-shaking (A2 equivalent) |
| VIII — Auth & AuthZ | ➖ N/A | — | No protected endpoints added |
| IX — API Design Conventions | ➖ N/A | — | No new API endpoints |
| X — Testing Policy | ✅ Partial | ✅ PASS | `kotlin.test` runs on shared KMP code; visual correctness verified via Dev Playground; no JVM Kotest tests needed for frontend-only work |
| Secrets & Environment Policy | ✅ Yes | ✅ PASS | No new secrets; fonts self-hosted (no CDN key needed) |

**Gate result**: ALL PASS — proceed to implementation.

## Project Structure

### Documentation (this feature)

```text
specs/002-design-system/
├── plan.md              ← this file
├── spec.md              ← feature specification
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
└── tasks.md             ← /speckit-tasks output (not created by /speckit-plan)
```

### Source Code Changes

```text
gradle/
└── libs.versions.toml              ← +gettext, +tailwindcss versions; +kilua-tailwindcss,
                                       kilua-i18n, kilua-fontawesome library entries;
                                       +gettext plugin entry

app/webApp/
├── build.gradle.kts                ← +kilua-tailwindcss, kilua-i18n, kilua-fontawesome deps;
│                                      +gettext plugin; +vite { plugin("@tailwindcss/vite") }
├── package.json                    ← +@fontsource/* packages, +highlight.js
├── vite.config.ts                  ← add <link rel=preload> hints (via transformIndexHtml)
└── src/
    └── commonMain/
        ├── resources/
        │   ├── index.html          ← +font preload links
        │   ├── tailwind.css        ← new: @theme tokens, font imports, dark/RTL variants
        │   ├── tailwind.config.js  ← new: { content: { files: ["SOURCES"] } }
        │   └── modules/i18n/
        │       ├── messages.pot    ← auto-generated by ./gradlew gettext
        │       ├── messages-en.po  ← English PO file (required by kilua-i18n)
        │       └── messages-fa.po  ← Persian translations
        └── kotlin/dev/kodex/webapp/
            ├── App.kt              ← update: init ThemeManager, I18n, locale dir watcher
            ├── design/
            │   ├── tokens/         ← DesignTokens.kt (Kotlin constants mirroring CSS vars)
            │   ├── theme/
            │   │   └── Theme.kt    ← ThemeMode enum, ThemeManager wiring
            │   ├── i18n/
            │   │   └── I18nSetup.kt← I18n object with en+fa PO modules
            │   ├── breakpoint/
            │   │   └── Breakpoint.kt← BreakpointTier enum, rememberBreakpoint()
            │   └── components/
            │       ├── Button.kt
            │       ├── Input.kt
            │       ├── TextArea.kt
            │       ├── Card.kt
            │       ├── Badge.kt
            │       ├── CodeBlock.kt
            │       ├── NavBar.kt
            │       ├── Sidebar.kt
            │       ├── Modal.kt
            │       ├── Toast.kt
            │       ├── LanguageSwitcher.kt
            │       └── ThemeSwitcher.kt
    └── webMain/
        └── kotlin/dev/kodex/webapp/
            ├── Main.kt             ← update: add TailwindcssModule, FontAwesomeModule to startApplication()
            └── playground/         ← dev-only (guarded by import.meta.env.DEV)
                ├── PlaygroundApp.kt
                ├── PlaygroundRegistry.kt
                └── previews/
                    ├── ButtonPreview.kt
                    ├── InputPreview.kt
                    └── ... (one per component)
```

**Structure Decision**: All design system code lives in `app/webApp/src/commonMain/kotlin/dev/kodex/webapp/design/`. Platform-specific code (locale dir, playground guard) goes in `webMain`. No new Gradle modules are needed — the design system is not a separate library module in v1 (it will be if a second frontend client is added).

## Complexity Tracking

No Constitution violations. No complexity justification required.
