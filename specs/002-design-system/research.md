# Research: Client-Side Design System

**Feature**: 002-design-system | **Phase**: 0 | **Date**: 2026-05-23

---

## Library Versions (new additions for this feature)

| Library | Version | Artifact |
|---|---|---|
| kilua-tailwindcss | **0.0.34** | `dev.kilua:kilua-tailwindcss` |
| kilua-i18n | **0.0.34** | `dev.kilua:kilua-i18n` |
| kilua-fontawesome | **0.0.34** | `dev.kilua:kilua-fontawesome` |
| kotlinx-gettext (plugin) | **0.7.0** | `name.kropp.kotlinx-gettext` |
| tailwindcss (npm) | **4.0.13** | `tailwindcss` (npm, via Kilua vite plugin) |
| @fontsource/vazirmatn (npm) | **5.1.1** | `@fontsource/vazirmatn` (npm) |
| @fontsource/inter (npm) | **5.1.1** | `@fontsource/inter` (npm) |
| @fontsource/jetbrains-mono (npm) | **5.1.1** | `@fontsource/jetbrains-mono` (npm) |

All `kilua-*` sub-modules track the same version as the core `kilua` library (`0.0.34`).
Font packages via `@fontsource/*` are self-hosted npm packages — preferred over Google Fonts CDN
for offline dev compatibility and build reproducibility.

---

## Decision 1 — TailwindCSS Integration: Vite Plugin Path

**Decision**: Use `@tailwindcss/vite` Vite plugin via Kilua's `vite { plugin(...) }` DSL block in `app/webApp/build.gradle.kts`.

**Rationale**: The project uses Vite 8 as its bundler (`app/webApp/vite.config.ts` confirmed). TailwindCSS v4 ships a dedicated `@tailwindcss/vite` plugin that integrates with Vite's transform pipeline. The Kilua Gradle plugin exposes a `vite { plugin("@tailwindcss/vite", "tailwindcss", version) }` DSL that installs the npm package and wires the plugin.

The setup requires three files:
1. `app/webApp/build.gradle.kts` — add `vite { plugin(…) }` block + `kilua-tailwindcss` dep
2. `app/webApp/src/commonMain/resources/tailwind.config.js` — `{ content: { files: ["SOURCES"] } }`
3. `app/webApp/src/commonMain/resources/tailwind.css` — Tailwind v4 `@import "tailwindcss"` + design tokens as `@theme` variables

**Alternatives considered**:
- Webpack PostCSS path: not applicable — project is Vite-based.
- Bootstrap CSS: rejected — too opinionated and conflicts with custom token system.

**Constitution note**: Adding `vite { }` config directly to `app/webApp/build.gradle.kts` is permitted under A3. A3 prohibits adding *quality tooling* (detekt, kover, benchmark) to module files; Tailwind is UI tooling specific to this module. No convention plugin update needed.

---

## Decision 2 — Font Loading: @fontsource npm Packages

**Decision**: Load all three fonts via `@fontsource/*` npm packages imported in `tailwind.css`.

**Rationale**: `@fontsource` packages are self-contained CSS + font file bundles installed via npm. They integrate directly with Vite's asset pipeline (hash-named files, browser caching, no external CDN dependency). Fonts are bundled into the production artifact. This is more reliable than Google Fonts CDN in CI, offline dev, and air-gapped production environments.

Import pattern in `tailwind.css`:
```css
@import "@fontsource/inter/variable.css";
@import "@fontsource/vazirmatn/arabic-variable.css";
@import "@fontsource/jetbrains-mono/variable.css";
```

**Alternatives considered**:
- Google Fonts `@import url(...)`: rejected — CDN dependency, blocked in some networks, not reproducible.
- CSS `<link>` tags in `index.html`: acceptable but breaks encapsulation; CSS import is cleaner.

---

## Decision 3 — Design Tokens: Tailwind CSS `@theme` Variables

**Decision**: Express all design tokens as CSS custom properties inside a Tailwind v4 `@theme { }` block in `tailwind.css`.

**Rationale**: TailwindCSS v4 introduces the `@theme` directive which maps CSS custom properties to Tailwind utility classes. This eliminates the `tailwind.config.js` `theme.extend` pattern and makes all tokens available as both utility classes (`bg-primary`) and raw CSS variables (`var(--color-primary)`). Dark mode is handled via Tailwind's `@custom-variant dark` and the `ThemeManager` adding/removing a `.dark` class on the root element.

Example token structure:
```css
@theme {
  --color-primary: #7F52FF;
  --color-primary-dark: #9D71FF;
  --color-accent: #F57D42;
  --color-surface: #FAFAFA;
  --color-surface-dark: #13111C;
  /* … */
  --font-sans: "Inter", "Vazirmatn", sans-serif;
  --font-mono: "JetBrains Mono", monospace;
}
.dark { --color-primary: #9D71FF; /* dark overrides */ }
[dir=rtl] { --font-sans: "Vazirmatn", sans-serif; }
```

**Alternatives considered**:
- CSS-in-JS / inline styles: rejected — conflicts with Kilua's class-based styling API.
- SCSS variables: rejected — Vite + Tailwind v4 has no SCSS processing step.

---

## Decision 4 — i18n: kilua-i18n + kotlinx-gettext

**Decision**: Use `kilua-i18n` module with `kotlinx-gettext` Gradle plugin. English string literals in source; Persian translations in `messages-fa.po`.

**Rationale**: Kilua's `I18n` class uses gettext PO file format — a mature, widely-tooled standard. The `kotlinx-gettext` Gradle plugin (`./gradlew gettext`) auto-extracts all `tr(...)` calls into `messages.pot`. The `@Composable` methods in `I18n` automatically recompose all affected UI when `LocaleManager.setCurrentLocale()` is called — zero manual refresh needed.

RTL activation: The `LocaleManager.setCurrentLocale()` call must also set `document.dir = "rtl"` (for `fa`) or `"ltr"` (for `en`). This is a one-line `jsMain` side effect guarded by `if (renderConfig.isDom)`.

Translation files location: `app/webApp/src/commonMain/resources/modules/i18n/`
- `messages.pot` — auto-generated by `./gradlew gettext`
- `messages-en.po` — English (same strings; required by kilua-i18n initialisation)
- `messages-fa.po` — Persian translations

**Alternatives considered**:
- Kotlin-ICU / manual string maps: rejected — no auto-extraction, no plural forms, not gettext standard.
- JS i18n library (i18next): rejected — violates §I Kotlin-First; introduces non-Kotlin runtime dependency.

---

## Decision 5 — RTL Layout: Tailwind `rtl:` Variant + CSS `[dir]` Attribute

**Decision**: Use Tailwind v4's built-in `rtl:` variant for layout mirroring; set `dir` attribute on the `<html>` element.

**Rationale**: Tailwind v4 supports `rtl:` and `ltr:` variants that apply when the nearest ancestor (or root) has `dir="rtl"`. Combined with the `@custom-variant dark` pattern for dark mode, this provides complete layout and direction control from utility classes alone. Logical CSS properties (`ms-`, `me-`, `ps-`, `pe-`) replace directional margin/padding (`ml-`, `mr-`) throughout components.

Kilua `jsMain` locale change hook:
```kotlin
LocaleManager.currentLocale.collectAsState().let { locale ->
    if (renderConfig.isDom) {
        document.documentElement?.dir = if (locale.language == "fa") "rtl" else "ltr"
    }
}
```

---

## Decision 6 — Dev Playground: Vite `import.meta.env.DEV` Tree-shaking

**Decision**: Gate all playground code behind `js("import.meta.env.DEV").unsafeCast<Boolean>()` in `jsMain`/`wasmJsMain`. Vite replaces this with `false` in production builds; rollup tree-shakes the unreachable code out.

**Rationale**: The spirit of A2 (dev-only code excluded from production artifact) applies. While the server:app module uses a Gradle `devMain` source set, the webApp's Vite production build achieves the same isolation via compile-time constant folding — a pattern idiomatic to Vite. The result is identical: zero playground bytes in the production bundle. A new `webDevMain` source set convention is not warranted for a single-module use case.

Playground entry point in `app/webApp/src/webMain/kotlin/dev/kodex/webapp/PlaygroundRegistry.kt`:
```kotlin
fun maybeRegisterPlayground(router: Router) {
    if (js("import.meta.env.DEV").unsafeCast<Boolean>()) {
        router.route("/playground") { PlaygroundApp() }
    }
}
```

Verification: `./gradlew :app:webApp:jsBrowserProductionWebpack` then `grep -r "PlaygroundApp" build/` → should return nothing.

---

## Decision 7 — CodeBlock Syntax Highlighting: Highlight.js CSS-only

**Decision**: Include Highlight.js CSS theme file; register `hljs.highlightAll()` via Kilua's `LaunchedEffect` on CodeBlock mount. Use `highlight.js@11.9.1`.

**Rationale**: Highlight.js is the most widely-used, production-stable syntax highlighting library. CSS-only themes allow static renders; the auto-detect feature handles Kotlin, Java, JSON, shell, etc. without per-language configuration. The JS runtime is small (~50 KB gzipped for common language pack).

**Alternatives considered**:
- Prism.js: comparable feature set; less maintained as of 2025.
- Shiki: SSR-first, requires WASM in browser; overhead unjustified for this use case.
- Pure CSS (no library): insufficient for multi-language support.

---

## Decision 8 — ThemeSwitcher Icons: kilua-fontawesome

**Decision**: Use `kilua-fontawesome` for ThemeSwitcher icons (sun/moon/auto). Adds `kilua-fontawesome:0.0.34` dep.

**Rationale**: Kilua's built-in `themeSwitcher()` composable uses Font Awesome icons by default. Using the same composable avoids re-implementing icon logic. FontAwesome 6 Free tier includes the required sun/moon icons. The `kilua-fontawesome` module is a thin wrapper — negligible bundle size impact.

**Alternatives considered**:
- Bootstrap Icons (kilua-bootstrap-icons): equally valid but less icon variety for theming metaphors.
- Unicode emoji (☀/☽): accessible but visually inconsistent with the design language.

---

## Summary: New `libs.versions.toml` Entries Required

```toml
[versions]
gettext        = "0.7.0"
tailwindcss    = "4.0.13"

[libraries]
kilua-tailwindcss = { module = "dev.kilua:kilua-tailwindcss",  version.ref = "kilua" }
kilua-i18n        = { module = "dev.kilua:kilua-i18n",         version.ref = "kilua" }
kilua-fontawesome = { module = "dev.kilua:kilua-fontawesome",  version.ref = "kilua" }

[plugins]
gettext = { id = "name.kropp.kotlinx-gettext", version.ref = "gettext" }
```

New npm packages in `app/webApp/package.json` (added via Kilua Gradle plugin automatically when using `vite { plugin(...) }`; font packages added manually):
- `tailwindcss: ^4.0.13`
- `@fontsource/inter: ^5.1.1`
- `@fontsource/vazirmatn: ^5.1.1`
- `@fontsource/jetbrains-mono: ^5.1.1`
- `highlight.js: ^11.9.1`
