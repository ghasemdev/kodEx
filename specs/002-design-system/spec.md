# Feature Specification: Client-Side Design System

**Feature Branch**: `feature/002-design-system`

**Created**: 2026-05-23

**Status**: Draft

**Input**: User description: "Feature 002: Client-Side Design System — Build a complete design system for the KodEx web frontend (app/webApp) using Kilua + TailwindCSS v4 with theme, fonts, i18n (EN/FA), responsive layout, component library, and dev playground."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Developer Applies Theme & Tokens (Priority: P1)

A developer building a new screen in KodEx opens the design token reference and applies the correct primary color, surface color, and typography scale to a new UI element. They switch between light and dark mode and see the UI update immediately without a page reload.

**Why this priority**: Theme tokens are the foundation of every other component and screen. Nothing else can be consistently styled until tokens exist.

**Independent Test**: Can be tested by rendering any single component (e.g., a Button) in both light and dark modes and verifying the correct palette is applied. Delivers a visually consistent baseline.

**Acceptance Scenarios**:

1. **Given** the app is running in light mode, **When** the user switches to dark mode via the ThemeSwitcher, **Then** all surfaces, text, and border colors update to the dark palette within one render cycle.
2. **Given** the user's OS prefers dark mode, **When** the app loads for the first time, **Then** the app defaults to dark mode automatically.
3. **Given** the user has previously selected light mode, **When** they reload the page, **Then** their preference is restored from local storage.
4. **Given** the app is in any theme, **When** a developer inspects a component, **Then** all colors reference CSS custom properties (design tokens), not hard-coded hex values.

---

### User Story 2 — Developer Uses Component Library (Priority: P2)

A developer needs a "Submit" button on the exam submission form. They open the component catalog, pick `Button` with variant `primary` and size `lg`, and drop it into their screen. The button is accessible, responds to hover/focus/disabled states, and looks correct in both themes.

**Why this priority**: Components are the primary building blocks for all future features. A complete, documented set prevents ad-hoc styling across the codebase.

**Independent Test**: Render each component in isolation in the Dev Playground. Each component is independently testable and delivers visual and interactive value on its own.

**Acceptance Scenarios**:

1. **Given** a Button with variant `primary`, **When** it is rendered, **Then** it displays the primary brand color, correct padding for its size, and a visible focus ring on keyboard focus.
2. **Given** a Button in `disabled` state, **When** the user attempts to click it, **Then** no action fires and the button is visually distinct from the enabled state.
3. **Given** a CodeBlock component, **When** code text is passed to it, **Then** it renders in a monospace typeface with horizontal scroll on overflow.
4. **Given** a Modal is triggered, **When** the user presses Escape or clicks the backdrop, **Then** the modal closes and focus returns to the triggering element.
5. **Given** a Toast notification is displayed, **When** no user interaction occurs, **Then** it auto-dismisses after a configurable duration (default 4 seconds).

---

### User Story 3 — User Switches Language (Priority: P2)

A Persian-speaking user opens KodEx and sees the interface in English (default). They click the LanguageSwitcher, select "فارسی", and the entire UI re-renders in Persian with correct right-to-left layout — text, icons, and navigation all mirror correctly.

**Why this priority**: RTL support affects every component and layout. Implementing it late causes widespread rework.

**Independent Test**: Render the NavBar + a Card with body text. Switch locale to `fa`. Verify text changes, layout mirrors, and font switches to Vazirmatn.

**Acceptance Scenarios**:

1. **Given** the app is in English (LTR), **When** the user selects Persian, **Then** all text strings update to Persian, the document direction becomes `rtl`, and the Vazirmatn font is applied.
2. **Given** the app is in Persian (RTL), **When** the LanguageSwitcher is displayed, **Then** the switcher itself is also mirrored (aligned to the correct edge).
3. **Given** the user selects Persian, **When** they reload the page, **Then** their language preference is restored.
4. **Given** the browser locale is `fa`, **When** the app loads for the first time, **Then** Persian is selected automatically.

---

### User Story 4 — Responsive Layout Across Devices (Priority: P3)

A participant opens KodEx on their phone. The navigation collapses to a bottom bar, cards stack vertically, and all inputs are full-width. On a tablet, the sidebar collapses to an icon rail. On a large TV screen, typography scales up fluidly and the content container has a wider max-width.

**Why this priority**: Responsive behavior is a cross-cutting concern but lower priority than the core component library, which it depends on.

**Independent Test**: Open the Dev Playground breakpoint switcher and toggle through Mobile / Tablet / Desktop / TV viewports. Each layout variant renders without overflow or broken layouts.

**Acceptance Scenarios**:

1. **Given** a viewport width below 640px, **When** any page is rendered, **Then** the navigation renders as a bottom bar and all cards are full-width single-column.
2. **Given** a viewport width between 640px and 1024px, **When** a page with a sidebar is rendered, **Then** the sidebar is collapsed and toggleable.
3. **Given** a viewport width above 1920px, **When** text-heavy content is displayed, **Then** font sizes scale up using fluid typography (no text appears disproportionately small).
4. **Given** any viewport, **When** the user rotates the device or resizes the browser, **Then** the layout re-adapts without requiring a page reload.

---

### User Story 5 — Developer Uses Dev Playground (Priority: P3)

A developer runs the app in local dev mode and clicks the "Components" menu in the playground sidebar. They select "Button" and see all variants and sizes rendered side-by-side with controls to toggle props. They also see a breakpoint switcher to preview each component at Mobile / Tablet / Desktop / TV sizes.

**Why this priority**: The playground accelerates development and design review but has no impact on production users.

**Independent Test**: Run `devRun`, navigate to the playground, and verify all 13 components are listed and render in the playground canvas.

**Acceptance Scenarios**:

1. **Given** the app is running in dev mode, **When** the developer navigates to the root URL, **Then** the playground sidebar is visible.
2. **Given** the playground is open, **When** the developer selects a component from the sidebar, **Then** all variants of that component are rendered in the main canvas.
3. **Given** the app is built for production, **When** the production bundle is inspected, **Then** no playground code or routes are present.
4. **Given** the playground is open, **When** the developer uses the breakpoint switcher, **Then** the canvas resizes to simulate the selected viewport.

---

### Edge Cases

- What happens when a translation key is missing for the selected locale? → Fall back to the English string silently; do not display raw key names.
- What happens when both OS theme preference and saved preference exist? → Saved user preference takes priority.
- What happens when a user's browser does not support CSS custom properties? → Out of scope; only modern browsers (ES2022+) are supported per the project's existing baseline.
- What happens when a component receives no required props? → Render a visible error placeholder in dev mode; render nothing (empty fragment) in production.
- What happens when the playground is accessed in production accidentally? → The routes do not exist in the production build; the router returns a 404.
- What happens when the viewport is between TV breakpoint and a very large monitor (e.g., 4K)? → `clamp()` fluid typography continues to scale up to a defined maximum value.

---

## Requirements *(mandatory)*

### Functional Requirements

**Theme System**

- **FR-001**: The system MUST provide a complete set of design tokens for color (primary, accent, surface, on-surface, error, success, warning), spacing, border radius, and shadow — all expressed as CSS custom properties.
- **FR-002**: The system MUST support two color modes — light and dark — with the ability to switch at runtime without a page reload.
- **FR-003**: The system MUST persist the user's color mode preference across sessions.
- **FR-004**: The system MUST detect the user's OS color scheme preference and apply it on first load if no saved preference exists.

**Typography & Fonts**

- **FR-005**: The system MUST load and apply Inter as the primary typeface for English UI text.
- **FR-006**: The system MUST load and apply Vazirmatn as the primary typeface for Persian UI text.
- **FR-007**: The system MUST load and apply JetBrains Mono as the typeface for all code display elements.
- **FR-008**: The system MUST switch the active font automatically when the application locale changes.

**Internationalisation**

- **FR-009**: The system MUST support English (`en`) and Persian (`fa`) locales.
- **FR-010**: The system MUST set the document text direction to `ltr` for English and `rtl` for Persian.
- **FR-011**: The system MUST detect the browser's locale on first load and pre-select the matching supported language (defaulting to English if unsupported).
- **FR-012**: The system MUST persist the user's language preference across sessions.
- **FR-013**: When a translation key is missing, the system MUST fall back to the English string without displaying raw key identifiers.

**Component Library**

- **FR-014**: The system MUST provide a Button component with five variants (`primary`, `secondary`, `ghost`, `danger`, `link`) and three sizes (`sm`, `md`, `lg`).
- **FR-015**: The system MUST provide an Input component supporting text, password, email, and number types, with optional label, helper text, and error state.
- **FR-016**: The system MUST provide a TextArea component with auto-resize-on-content behavior and optional character counter.
- **FR-017**: The system MUST provide a Card component with optional header, body, footer, and click-to-select variant.
- **FR-018**: The system MUST provide a Badge component with semantic color variants (default, success, warning, error, info).
- **FR-019**: The system MUST provide a CodeBlock component that renders monospaced text with horizontal scroll on overflow and a copy-to-clipboard action.
- **FR-020**: The system MUST provide a NavBar component that collapses to a bottom navigation bar on mobile viewports.
- **FR-021**: The system MUST provide a Sidebar component that collapses to an icon rail on tablet viewports and is hidden (replaced by bottom nav) on mobile.
- **FR-022**: The system MUST provide a Modal component that traps focus while open, closes on Escape key or backdrop click, and returns focus to the trigger on close.
- **FR-023**: The system MUST provide a Toast/Snackbar component that stacks multiple notifications, auto-dismisses after a configurable duration, and supports manual dismiss.
- **FR-024**: The system MUST provide a LanguageSwitcher component that lists available locales and updates the app locale on selection.
- **FR-025**: The system MUST provide a ThemeSwitcher component that cycles through light, dark, and auto (OS preference) modes.
- **FR-026**: All components MUST apply correct styles in both light and dark modes using the design token system.
- **FR-027**: All components MUST mirror their layout correctly in RTL mode.

**Responsive Design**

- **FR-028**: All layouts MUST be functional and visually correct at four breakpoint tiers: Mobile (`< 640px`), Tablet (`640px–1024px`), Desktop (`1024px–1920px`), and TV/Large (`> 1920px`).
- **FR-029**: On Mobile, the primary navigation MUST be a bottom bar; cards MUST stack in a single column; inputs MUST be full-width.
- **FR-030**: On Tablet, the sidebar MUST be collapsible; grid layouts MUST use a two-column arrangement.
- **FR-031**: On TV/Large viewports, the typography MUST scale fluidly using `clamp()` to a defined maximum; the content container MUST have a wider max-width.

**Dev Playground**

- **FR-032**: The playground MUST be compiled only in the `devMain` source set and MUST NOT appear in the production build artifact.
- **FR-033**: When running in dev mode, the playground MUST be accessible as a sidebar menu visible on the root route.
- **FR-034**: The playground sidebar MUST list all 13 design system components by name; selecting any component MUST render all its variants and interactive states in the main canvas.
- **FR-035**: The playground MUST include a breakpoint switcher that resizes the canvas to simulate Mobile, Tablet, Desktop, and TV viewports.

### Key Entities

- **DesignToken**: A named CSS custom property representing a single visual decision (color, spacing, radius, shadow). Has a light-mode value and a dark-mode value.
- **Locale**: A supported language identifier (`en`, `fa`) with associated translation file, text direction (`ltr`/`rtl`), and font family.
- **Component**: A reusable, self-contained UI element. Has a name, a set of variants, a set of sizes (where applicable), and a set of interactive states (default, hover, focus, disabled, error).
- **Breakpoint**: A named viewport width tier (Mobile, Tablet, Desktop, TV) that triggers layout adaptations.
- **PlaygroundEntry**: A dev-only registry entry mapping a component name to its rendered variants for the playground canvas.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can adopt any component from the design system into a new screen without writing any custom CSS — 100% of component styling uses design tokens and Tailwind utility classes.
- **SC-002**: Switching between light and dark mode takes less than one render frame (no flash of unstyled content or visible delay).
- **SC-003**: Switching between English and Persian locales causes the full UI to re-render in the correct language, direction, and font within one render cycle — no page reload required.
- **SC-004**: All 13 components render without visual defects at all four breakpoint tiers (Mobile, Tablet, Desktop, TV), verified in the Dev Playground.
- **SC-005**: The production build artifact contains zero bytes of playground code (verified by bundle analysis).
- **SC-006**: All components pass keyboard-navigation and ARIA accessibility checks (focus management, role attributes, contrast ratio ≥ 4.5:1 for normal text).
- **SC-007**: A new developer can locate and use any component by browsing the Dev Playground in under 2 minutes without external documentation.

---

## Assumptions

- The existing Vite build setup in `app/webApp/build.gradle.kts` supports adding the `@tailwindcss/vite` plugin without changes to the convention plugin (A3); if it does not, the convention plugin will be updated, not the module build file directly.
- Google Fonts are accessible in the target deployment environment; if not, fonts will be self-hosted under `src/commonMain/resources/fonts/`.
- Syntax highlighting for CodeBlock is handled via CSS classes (e.g., highlight.js theme) rather than a runtime JS library, keeping the bundle size minimal.
- The `devMain` source set pattern established in `server:app` (A2) applies equivalently to `app:webApp` — the playground is placed in a `devMain` source set that is excluded from the production Vite/Kotlin build.
- The design system covers the `app/webApp` module only; Android and other future clients are out of scope for this feature.
- No third-party component library (e.g., shadcn, DaisyUI) is used; all components are built from Tailwind utility classes in Kilua's DSL to maintain full control and avoid CSS conflicts.
- The initial component set (13 components) is sufficient for MVP feature development; additional components will be added in future specs as needed.
- Accessibility compliance targets WCAG 2.1 AA as a baseline.
