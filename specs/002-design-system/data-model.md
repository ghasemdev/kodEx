# Data Model: Client-Side Design System

**Feature**: 002-design-system | **Phase**: 1 | **Date**: 2026-05-23

No server-side schema changes. All entities in this document are **client-side only** and live
in `app/webApp/src/commonMain/kotlin/dev/kodex/webapp/design/`.

---

## 1. DesignToken

Represents a single visual decision expressed as a CSS custom property.

| Field | Type | Notes |
|---|---|---|
| `name` | `String` | CSS variable name, e.g., `--color-primary` |
| `lightValue` | `String` | CSS value in light mode, e.g., `#7F52FF` |
| `darkValue` | `String` | CSS value in dark mode, e.g., `#9D71FF` |
| `category` | `TokenCategory` | `Color`, `Spacing`, `Radius`, `Shadow`, `Typography` |

Tokens are **not Kotlin objects** — they are declared in `tailwind.css` under `@theme { }`.
This model is documentation-only to establish the full token vocabulary.

### Token Inventory

#### Color Tokens

| Token | Light | Dark | Usage |
|---|---|---|---|
| `--color-primary` | `#7F52FF` | `#9D71FF` | Kotlin Purple — primary brand action |
| `--color-primary-hover` | `#6B40F0` | `#B392FF` | Button hover state |
| `--color-accent` | `#F57D42` | `#FF9060` | Kotlin Orange — highlights, badges |
| `--color-surface` | `#FAFAFA` | `#13111C` | Page background |
| `--color-surface-2` | `#F0EFF5` | `#1E1A2E` | Card / panel background |
| `--color-surface-3` | `#E4E2EF` | `#2A2440` | Input background |
| `--color-on-surface` | `#1A1625` | `#EDE9FF` | Primary text color |
| `--color-on-surface-muted` | `#6B6580` | `#9B96B8` | Secondary / helper text |
| `--color-border` | `#D0CDE8` | `#3A3457` | Input / card borders |
| `--color-error` | `#E24462` | `#FF6B8A` | Error state |
| `--color-success` | `#22C55E` | `#4ADE80` | Success state |
| `--color-warning` | `#F59E0B` | `#FCD34D` | Warning state |
| `--color-info` | `#3B82F6` | `#60A5FA` | Info state |
| `--color-code-bg` | `#F4F3FF` | `#0D0B18` | CodeBlock background |

#### Typography Tokens

| Token | Value | Usage |
|---|---|---|
| `--font-sans` | `"Inter", "Vazirmatn", sans-serif` | UI text (switches per locale) |
| `--font-mono` | `"JetBrains Mono", monospace` | Code, input with `type=number` |
| `--text-xs` | `clamp(0.7rem, 0.65rem + 0.25vw, 0.85rem)` | Badge labels |
| `--text-sm` | `clamp(0.8rem, 0.75rem + 0.25vw, 1rem)` | Helper text, captions |
| `--text-base` | `clamp(0.875rem, 0.8rem + 0.375vw, 1.125rem)` | Body text |
| `--text-lg` | `clamp(1rem, 0.9rem + 0.5vw, 1.375rem)` | Section headings |
| `--text-xl` | `clamp(1.125rem, 1rem + 0.625vw, 1.625rem)` | Page headings |
| `--text-2xl` | `clamp(1.375rem, 1.2rem + 0.875vw, 2rem)` | Hero headings |

`clamp()` provides fluid scaling across all four breakpoint tiers (Mobile → TV) automatically.

#### Spacing & Radius Tokens

| Token | Value | Usage |
|---|---|---|
| `--spacing-px` | `1px` | Hairline borders |
| `--radius-sm` | `0.25rem` | Badge, chip |
| `--radius-md` | `0.5rem` | Input, button |
| `--radius-lg` | `0.75rem` | Card |
| `--radius-xl` | `1rem` | Modal |
| `--radius-full` | `9999px` | Avatar, toggle |
| `--shadow-sm` | `0 1px 3px rgba(0,0,0,.08)` | Card resting |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,.12)` | Modal, dropdown |
| `--shadow-lg` | `0 8px 24px rgba(0,0,0,.16)` | Toast, popover |

---

## 2. Locale

Represents a supported application language.

| Field | Type | Notes |
|---|---|---|
| `code` | `String` | BCP-47 language tag: `"en"`, `"fa"` |
| `displayName` | `String` | Localized name: `"English"`, `"فارسی"` |
| `direction` | `TextDirection` | `LTR` or `RTL` |
| `fontFamily` | `String` | Primary font family name |
| `poResource` | `JsAny` | External JS module reference to `.po` file |

State transitions: `LocaleManager.currentLocale` flows through `StateFlow<SimpleLocale>`. Changes trigger:
1. `I18n` recompose cascade (all `tr()` calls update)
2. `document.documentElement?.dir` updated (`jsMain` only, guarded by `renderConfig.isDom`)
3. CSS `[dir=rtl]` variant activates Tailwind RTL utilities

---

## 3. ThemeMode

Represents the active color scheme preference.

```kotlin
enum class ThemeMode { Light, Dark, Auto }
```

| Value | Behaviour |
|---|---|
| `Light` | Forces light palette; persisted to LocalStorage |
| `Dark` | Forces dark palette; persisted to LocalStorage |
| `Auto` | Follows `prefers-color-scheme` media query; no LocalStorage override |

Managed by Kilua's `ThemeManager` object. The `.dark` CSS class is toggled on `<html>` element.

---

## 4. ComponentVariant / ComponentSize

Documentation-level enumerations for the component library contract.

```kotlin
enum class ButtonVariant { Primary, Secondary, Ghost, Danger, Link }
enum class ComponentSize  { Sm, Md, Lg }
enum class BadgeVariant   { Default, Success, Warning, Error, Info }
enum class InputType      { Text, Password, Email, Number }
```

These are Kotlin `sealed class` / `enum class` definitions in `commonMain`. Each component
receives its variant/size as a typed parameter, enforcing valid combinations at compile time.

---

## 5. BreakpointTier

Runtime representation of the current viewport tier.

```kotlin
enum class BreakpointTier { Mobile, Tablet, Desktop, Tv }
```

| Tier | Condition |
|---|---|
| `Mobile` | viewport `< 640px` |
| `Tablet` | `640px ≤ viewport < 1024px` |
| `Desktop` | `1024px ≤ viewport ≤ 1920px` |
| `Tv` | viewport `> 1920px` |

Provided by `rememberBreakpoint(): State<BreakpointTier>` composable in `webMain`, using
`window.matchMedia` queries. Components read this for layout decisions that cannot be expressed
with Tailwind utilities alone (e.g., collapsing NavBar vs showing Sidebar at runtime).

---

## 6. ToastMessage

Transient notification displayed in the Toast/Snackbar component.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID, used as stable key for list rendering |
| `message` | `String` | Translated display string |
| `variant` | `BadgeVariant` | Controls background color |
| `durationMs` | `Int` | Auto-dismiss duration; default 4000 |
| `dismissible` | `Boolean` | Shows close button; default true |

`ToastStore` holds a `MutableList<ToastMessage>` in `remember {}` state. Items are removed either on timer expiry or on user dismiss.

---

## 7. PlaygroundEntry *(dev-only)*

Dev-only registry mapping component names to their playground renderers.

```kotlin
data class PlaygroundEntry(
    val name: String,
    val render: @Composable () -> Unit
)
```

`PlaygroundRegistry` is a `List<PlaygroundEntry>` populated at app startup, guarded by `import.meta.env.DEV`. It is never included in the production bundle.

---

## State Flows & Ownership

| State | Owner | Scope |
|---|---|---|
| `ThemeManager.theme` | Kilua `ThemeManager` (singleton) | App-wide |
| `LocaleManager.currentLocale` | Kilua `LocaleManager` (singleton) | App-wide |
| `BreakpointTier` | `rememberBreakpoint()` composable | Component subtree |
| `ToastStore.messages` | `ToastStore` (app-scoped) | App-wide |
| Playground selected component | `PlaygroundViewModel` | Playground subtree only |
