---
name: kilua-framework-expert
description: Advanced architectural, implementation, and debugging guide for Kilua framework. Mandatory for all UI, state, RPC, and fullstack logic generation.
compatibility: Requires Kilua-based project with documentation under .claude/skills/kilua-framework-skill/reference/
metadata:
  author: Kilua-Developer
  source: .claude/skills/kilua-framework-skill/SKILL.md
---

# Kilua Framework Comprehensive Skill Guide

## 1. Architectural Philosophy
Kilua is not a wrapper around React/Vue. It is a full-stack Kotlin framework powered by Compose Runtime.
- **Compose Runtime:** UI is always a projection of State. Never manipulate DOM directly.
- **Type-Safe Fullstack:** RPC is the primary communication channel. Use `@RpcService`.
- **SSR-First Design:** Write code assuming it will run on both JVM and JS/Wasm. Browser-specific APIs (`window`, `localStorage`) MUST be guarded.

## 2. Implementation Standards

### A. State Management
- Use `remember { mutableStateOf(...) }` for component-local state.
- Use `StateFlow` or observable objects for application-level state.
- **Avoid:** Global mutable variables. Always lift state up to the nearest common ancestor if multiple components share data.

### B. Fullstack & RPC
- **Client-to-Server:** Use `@RpcService` interfaces. 
- **Type Safety:** Never pass `dynamic` types across the network. Always use `@Serializable` data classes.
- **REST:** Only use `kilua-rest` when interacting with 3rd-party APIs. For internal communication, RPC is mandatory.

### C. UI & Styling
- **DSL over HTML:** Always use Kilua's DSL (`div`, `span`, `button`). Never write raw HTML strings.
- **Bootstrap/Tailwind:** Use the provided wrapper classes (e.g., `bsButton`, `vPanel`).
- **Responsive:** Use `rememberBootstrapBreakpoint()` to adapt UI.

### D. Routing
- **Hierarchy:** Prefer layout-based routing.
- **Navigation:** Use `navigate()` or `navLink`. Do not manipulate `window.location`.
- **Metadata:** Always provide proper metadata for SSR optimization.

## 3. Environment & SSR Guarding
Always check execution context:
```kotlin
if (renderConfig.isDom) {
    // Client-side only code (e.g., localStorage, window)
} else {
    // Server-side / SSR-only code
}
```

## 4. Documentation Reference Map (Reference Directory: ./reference/)
The following files must be consulted for specific implementation details:

### Foundations
- `introduction.md`, `compose-world.md`, `getting-started/setting-up.md`
- `getting-started/creating-a-new-application.md`, `getting-started/development-workflow.md`

### Core Development
- `development-guide/composable-functions.md`, `development-guide/working-with-compose.md`
- `development-guide/layout-containers.md`, `development-guide/rendering-html.md`
- `development-guide/events.md`, `development-guide/forms.md`, `development-guide/routing.md`

### Fullstack & Integration
- `development-guide/fullstack-components.md`, `development-guide/server-side-rendering.md`
- `development-guide/rest-client.md`, `development-guide/using-tabulator.md`
- `development-guide/using-tailwindcss.md`, `development-guide/using-bootstrap.md`

### Advanced
- `development-guide/interoperability-with-javascript.md`, `development-guide/ktml-templates.md`
- `development-guide/animation.md`, `development-guide/svg-images.md`
- `development-guide/debugging.md`, `development-guide/internationalization.md`

## 5. Strict Prohibitions
1. **NO DOM Manipulation:** No `document.getElementById`, `querySelector`, etc.
2. **NO React/JSX:** Kilua is NOT React. Do not suggest hooks like `useEffect` or `useMemo` from React.
3. **NO raw JS:** Avoid `js()` blocks unless strictly necessary for third-party interop.
4. **NO Manual API calls:** Use `RestClient` or `RPC`.

## 6. Interaction Protocol
1. **Search First:** If asked a technical question, search the `./reference/` folder first.
2. **Context Awareness:** When providing code, specify if it belongs to `commonMain`, `jsMain`, or `jvmMain`.
3. **Validation:** Ensure all inputs are validated using Kilua form validators.
4. **Gradle:** Always suggest Gradle tasks for build/run issues.

---
*Failure to comply with these rules results in technical debt. When in doubt, read the linked markdown files.*
```
