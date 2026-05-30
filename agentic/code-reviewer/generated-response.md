# AI Code Review

```markdown
- file: app/webApp/src/webMain/kotlin/dev/kodex/webapp/App.kt
  line: 30
  issue: Coroutine misuse / Logic bug
  suggestion: The `MainScope().launch { initI18n() }` call uses a global `MainScope`. While acceptable for
  application-level initialization, if `initI18n()` involves significant I/O (e.g., loading translation files from
  network), launching it directly on `Dispatchers.Main` could potentially block the UI thread. Consider if `initI18n`
  itself should handle dispatching I/O work to a suitable dispatcher (like `ioDispatcher` if available in `webMain`
  context) or if the `launch` call should specify a different dispatcher if `initI18n` is a suspend function that
  doesn't handle its own dispatching.

- file: app/webApp/src/webMain/kotlin/dev/kodex/webapp/App.kt
  line: 35
  issue: Logic bug / State inconsistency
  suggestion: The line `LocaleManager.setCurrentLocale(LocaleManager.currentLocale)` is called unconditionally after
  `MainScope().launch { initI18n() }`. If `initI18n()` is asynchronous and is also responsible for setting the initial
  locale and `document.dir`, this explicit `setCurrentLocale` call might be redundant, cause a race condition, or
  unnecessarily trigger side effects. Ensure there's a clear responsibility for initial locale setup and `document.dir`
  application. If `initI18n` handles it, this line might be removable or should be called only after `initI18n` has
  completed its work.

- file: app/webApp/src/webMain/kotlin/dev/kodex/webapp/core/ViewModel.kt
  line: 11
  issue: Coroutine misuse / State inconsistency
  suggestion: The `ViewModel` class provides a `viewModelScope` and an `onCleared()` method to cancel it. However, the
  provided code does not show how `onCleared()` is guaranteed to be called when a `ViewModel` instance is no longer
  needed (e.g., when a Kilua/Compose component using it is disposed). Without proper lifecycle management, coroutines
  launched in `viewModelScope` will leak, leading to memory issues and potential updates to non-existent UI. Ensure that
  any UI component consuming a `ViewModel` calls `viewModel.onCleared()` when it leaves the composition (e.g., using
  `DisposableEffect` in Compose).

- file: app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/AppModule.kt
  line: 13
  issue: Logic bug / Coroutine misuse
  suggestion: The commented-out `factory<LandingViewModel>()` indicates that `LandingViewModel` instances will be
  created on demand. While `factory` is appropriate for ViewModels, it reinforces the need for explicit lifecycle
  management. Each `LandingViewModel` instance created via Koin's `factory` will have its own `viewModelScope`. If
  `onCleared()` is not called for each instance when it's no longer used, its coroutines will leak. This is a direct
  consequence of the issue identified in `ViewModel.kt`. When uncommenting, ensure proper lifecycle handling in the UI
  layer.
```

As a senior software architect, I've reviewed the provided code chunks with a focus on readability, design issues,
duplication, and clean architecture violations.

---

### Code Readability

1. **`AppDispatchers.kt` Suppressions**: The `@Suppress("PropertyName", "RedundantSuppression")` annotations on
   `ioDispatcher` are present across all platform-specific implementations. While `PropertyName` is understandable for a
   global accessor that isn't `UPPER_SNAKE_CASE`, `RedundantSuppression` suggests the linter might not always flag it.
   It's a minor point, but ensuring suppressions are truly necessary and well-justified improves code clarity.
2. **`App.kt` Labeled Expression Suppression**: The `@Suppress("LabeledExpression")` on the `App` class is unusual.
   Labeled expressions are typically used for `break` or `continue` in nested loops or `return` from specific lambdas.
   Its presence without an obvious labeled expression in the current code might indicate a leftover suppression or a
   pre-emptive one for future code. It slightly reduces readability by suggesting a complexity that isn't immediately
   apparent.
3. **`App.kt` Commented-Out Code**: The commented-out routing logic and page imports in `App.kt` and the Koin module in
   `AppModule.kt` are useful for showing intent but should be removed once the actual implementation is in place to keep
   the codebase clean.

### Design Issues

1. **Manual Client-Side Routing (`App.kt`)**:
    * **Issue**: The current implementation of client-side routing in `App.kt` relies on manually checking
      `window.location.pathname` and adding a `popstate` event listener. This is a very basic and manual approach.
    * **Impact**: It lacks features like route parameters, nested routes, programmatic navigation, and a clear
      separation of routing concerns. As the application grows, this manual approach will become difficult to maintain,
      extend, and test.
    * **Recommendation**: Adopt a dedicated routing solution. Kilua might offer its own router, or a third-party library
      could be integrated. This would centralize routing logic, improve maintainability, and provide a more robust
      navigation experience. The commented-out `when` block indicates a move in the right direction, but a proper router
      abstraction is needed.
2. **`ViewModel` Dispatcher Usage**:
    * **Issue**: The `ViewModel`'s `viewModelScope` is initialized with `Dispatchers.Main.immediate`. While this is
      suitable for UI-related coroutines, it implies that *all* coroutines launched directly within `viewModelScope`
      will run on the main thread.
    * **Impact**: If the `ViewModel` directly performs I/O operations (e.g., network requests, database access) or heavy
      computations without explicitly switching dispatchers, it could block the UI thread, leading to a frozen UI and
      poor user experience.
    * **Recommendation**: Ensure that I/O-bound or CPU-bound operations are delegated to lower layers (e.g.,
      repositories, use cases) which are responsible for switching to appropriate dispatchers like `ioDispatcher` (from
      `app/shared`) or `Dispatchers.Default`. The `ViewModel` should primarily orchestrate UI logic and state changes,
      not perform blocking operations itself.

### Duplication

1. **`AppDispatchers.kt` Suppressions**: The `@Suppress("PropertyName", "RedundantSuppression")` annotation is
   duplicated across `commonMain`, `jsMain`, `jvmMain`, and `wasmJsMain` for the `ioDispatcher` property.
    * **Impact**: While minor, it's a repetitive piece of code.
    * **Recommendation**: For `expect/actual` declarations, if the suppression is truly needed for the `expect`
      declaration, it might be inherited or a single suppression on the `expect` declaration could suffice, depending on
      the linter configuration. If not, it's a small, acceptable duplication.

### Clean Architecture Violations

1. **`ViewModel` and `ioDispatcher` (Potential Violation)**:
    * **Issue**: The `ioDispatcher` is correctly defined in the `app/shared` module, making it accessible across
      platforms. However, the `ViewModel`'s `viewModelScope` uses `Dispatchers.Main.immediate`. If the `ViewModel`
      directly initiates I/O operations without delegating to an intermediary layer (like a Use Case or Repository) that
      explicitly uses `ioDispatcher`, it could violate the separation of concerns inherent in Clean Architecture.
    * **Impact**: This would couple the `ViewModel` to implementation details of data fetching/storage and threading,
      making it harder to test, reuse, and maintain. It blurs the lines between presentation logic and data access
      logic.
    * **Recommendation**: Adhere to the principle that `ViewModel`s interact with Use Cases (or Interactors), which then
      coordinate with Repositories. Repositories are responsible for abstracting data sources and handling threading
      concerns (e.g., using `ioDispatcher` for network/database calls). The `ViewModel` should primarily observe data
      from Use Cases and update the UI state. The commented-out Koin setup for `LandingStatsRepository` and
      `LandingViewModel` suggests an intent to follow this pattern, which is good. Ensure this pattern is consistently
      applied.

```markdown
- file: app/webApp/src/webMain/kotlin/dev/kodex/webapp/gsap/GsapInterop.kt
  line: 25
  issue: Logic bug / Type mismatch
  suggestion: The `opacity` property in `GsapVars` is defined as `Int`. GSAP typically expects a `Double` (float) value
  between 0.0 and 1.0 for opacity. Using `Int` will lead to incorrect animation behavior, likely resulting in a binary
  opacity (fully transparent or fully opaque) instead of a smooth fade. Change `var opacity: Int` to
  `var opacity: Double`.

- file: app/webApp/src/webMain/kotlin/dev/kodex/webapp/playground/previews/GsapSmokePreview.kt
  line: 44
  issue: Logic bug / Type mismatch
  suggestion: Due to the `opacity: Int` definition in `GsapVars` (from `GsapInterop.kt`), passing `opacity = 0` and
  `opacity = 1` here will not result in a smooth fade as intended. After changing `opacity` to `Double` in `GsapVars`,
  update these values to `opacity = 0.0` and `opacity = 1.0` respectively to ensure correct animation.
```

The following observations are made based on the provided code chunks:

### File: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/NetworkModule.kt`

* **Design Issues**:
    * `expectSuccess = false`: Setting `expectSuccess = false` globally for `HttpClient` is a strong design choice that
      can lead to unexpected behavior if not carefully managed. It means that the client will not throw exceptions for
      non-2xx HTTP responses (e.g., 400, 500 errors). While this can be useful for specific endpoints where non-success
      codes are part of the expected flow, making it the default for *all* requests can hide actual network or API
      issues, requiring every API call to explicitly check the response status. It's generally safer to
      `expectSuccess = true` and handle specific non-success cases where needed, or create a separate client instance
      for those specific scenarios.

### File: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/gsap/GsapInterop.kt`

* **Code Readability / Design Issues**:
    * **Overly broad `@file:Suppress` annotations**: The file starts with
      `@file:Suppress("PropertyName", "FunctionMinLength", "VariableMinLength", "unused")`. While `unused` is common for
      external declarations, `PropertyName`, `FunctionMinLength`, and `VariableMinLength` suggest a deviation from
      standard Kotlin naming conventions or a need to suppress warnings for very short names. This can reduce
      readability if not justified, especially `FunctionMinLength` and `VariableMinLength`. It's worth evaluating if
      these suppressions are truly necessary or if the underlying naming could be improved.
    * **Restrictive `GsapVars` interface**: The `GsapVars` interface is defined with very specific properties (`x: Int`,
      `opacity: Int`, `duration: Int`, `ease: String`, `onComplete: () -> Unit`). GSAP is a highly flexible animation
      library with a vast array of properties (e.g., `y`, `scale`, `rotation`, `delay`, `stagger`, `onStart`,
      `onUpdate`, etc.). This current definition will require constant modification as more GSAP features are utilized,
      leading to a brittle API.
        * **Type Mismatch Potential**: `x: Int` and `opacity: Int` are defined as `Int`. GSAP typically uses `number` (
          float/double) for these properties. For example, `opacity` is usually between `0.0` and `1.0`. While
          JavaScript's type coercion might make `0` work for `0.0`, it's not type-safe and can lead to unexpected
          behavior or precision loss.
    * **Lack of Flexibility for GSAP Properties**: To make `GsapVars` more robust, consider:
        * Using `dynamic` or `JsPlainObject` if full type safety isn't critical for every property.
        * Creating a builder pattern or a more comprehensive interface that allows for a wider range of GSAP properties,
          potentially using a `Map<String, JsAny>` for less common ones.
        * Defining common properties with their correct types (e.g., `opacity: Double`).

### File: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/playground/previews/GsapSmokePreview.kt`

* **Code Readability / Design Issues**:
    * **`@file:Suppress("KotlinUnreachableCode", "LabeledExpression")`**: The `KotlinUnreachableCode` suppression is a
      red flag. It indicates that there might be dead code or a misunderstanding of control flow within the file. This
      should be investigated and resolved rather than suppressed, as unreachable code can be a source of bugs or
      indicate poor logic. `LabeledExpression` is less critical but still suggests a specific coding style.
    * **Direct `document.getElementById` usage**: The `LaunchedEffect` directly accesses
      `document.getElementById(boxId)`. In a declarative UI framework like Compose/Kilua, directly manipulating the DOM
      via `document.getElementById` is generally discouraged. The framework should ideally manage element references.
      This creates a tight coupling between the component's logic and the underlying DOM structure, making the component
      less portable and harder to test. A more idiomatic approach would involve using a `ref` system provided by the UI
      framework (if available) or passing the element reference through the component hierarchy.
    * **Direct `unsafeJso` usage**: The `unsafeJso` function is a low-level interop utility that bypasses Kotlin's type
      system. While necessary for some JS interop, its direct and frequent use within application logic (especially in a
      UI component) reduces type safety and makes the code harder to reason about and refactor. It would be better to
      encapsulate `unsafeJso` behind a more type-safe API or a builder pattern for `GsapVars` (as discussed in
      `GsapInterop.kt`).
    * **String concatenation for `className`**: The `className` string for the `div` is constructed using concatenation.
      For complex or conditional class names, this can become hard to read and maintain. Consider using a utility
      function or a DSL for CSS classes if this pattern is repeated across the codebase.

* **Clean Architecture Violations**:
    * **UI Component Directly Interacting with Infrastructure (JS Interop & DOM)**: The `GsapSmokePreview` Composable
      directly calls `gsap.fromTo` (a JS interop library) and `document.getElementById` (direct DOM manipulation). This
      violates the separation of concerns principle in Clean Architecture. A UI component should ideally focus on *what*
      to display and *what* user interactions to handle, not *how* to animate or *how* to manipulate the DOM.
        * **Recommendation**: Introduce an abstraction layer (e.g., a `GsapAnimationService` or a dedicated `Animator`
          component) that encapsulates the GSAP calls and DOM interactions. The UI component would then interact with
          this service/component declaratively, requesting an animation without knowing the underlying implementation
          details. This would make the UI component more testable, reusable, and independent of the specific animation
          library.

Here's an analysis of the provided roadmap document, focusing on potential NullPointerException risks, coroutine misuse,
logic bugs, and state inconsistency.

```markdown
- file: docs/roadmap.md
  line: 699
  issue: NullPointerException risk / Runtime error
  suggestion: The `editorEl: HTMLElement` parameter in `setupHeroAnimation` is non-nullable. If the element with the
  corresponding ID is not found in the DOM, the code attempting to retrieve it (not shown here) might return `null` or
  `undefined` from JavaScript. Passing such a value to a non-nullable Kotlin parameter would result in a
  `NullPointerException`. It's safer to either make `editorEl` nullable (`editorEl: HTMLElement?`) and handle the null
  case, or ensure the element's presence before calling the function.
- file: docs/roadmap.md
  line: 699
  issue: State inconsistency / Logic bug (minor)
  suggestion: The `Gsap.registerPlugin(ScrollTriggerPlugin)` call is placed inside `setupHeroAnimation`. If
  `setupHeroAnimation` is called multiple times (e.g., on route changes or component re-mounts), the plugin will be
  registered repeatedly. While GSAP's `registerPlugin` is generally idempotent, it's best practice to register plugins
  once globally at application startup to manage state consistently and avoid potential subtle issues or unnecessary
  overhead.
- file: docs/roadmap.md
  line: 699
  issue: Logic bug / Interop risk
  suggestion: The `dynamic` type for `targets` and `vars` in the GSAP interop functions bypasses static type checking.
  While necessary for flexible JS interop, it means the Kotlin compiler cannot verify the structure or correctness of
  the `js("{ opacity:1, duration:0.4 }")` object passed as `vars`. Incorrect property names, types, or missing required
  fields in these dynamic objects will lead to runtime JavaScript errors that are hard to catch at compile time. It's
  crucial to have thorough runtime tests for these interop calls.
- file: docs/roadmap.md
  line: 457
  issue: State inconsistency / Logic bug
  suggestion: The rule "Editing test cases forbidden once PUBLISHED" for exam creation is a critical state consistency
  constraint. The implementation must strictly enforce this on both the frontend (UI disablement) and backend (API
  validation). A failure to do so would allow `EXAM_CREATOR` or `ADMIN` roles to modify published exams, potentially
  compromising contest integrity.
- file: docs/roadmap.md
  line: 593
  issue: Logic bug / Ambiguity
  suggestion: The badge rule "🌙 Night Owl | Participate in any exam that starts after 00:00 local time" is ambiguous
  regarding "local time". This could refer to the user's local time, the server's local time, or a specific fixed
  timezone. If it's the user's local time, the server needs to reliably know and track the user's timezone, which can
  change. This ambiguity can lead to inconsistent badge awarding or unexpected behavior. The definition of "local time"
  should be explicitly clarified (e.g., UTC, server's timezone, or user-provided timezone).
- file: docs/roadmap.md
  line: 594
  issue: Logic bug / Ambiguity
  suggestion: The badge rule "🔥 Steady Hand | Participate in at least 1 exam per week for 5 consecutive weeks" needs a
  clear definition of "week" (e.g., Monday-Sunday, Sunday-Saturday, or a rolling 7-day period). The logic for tracking "
  consecutive weeks" also needs to be robust, handling edge cases like exams spanning week boundaries or slight delays
  in participation. An imprecise definition could lead to logic errors in badge awarding.
- file: docs/roadmap.md
  line: 596
  issue: Logic bug / Missing specification
  suggestion: The badge rule "🎲 Creative Chaos | Solution flagged 'unconventional' by the grader yet passes all tests"
  relies on a mechanism to "flag" a solution as "unconventional". This mechanism is not specified. Is it a manual flag
  by an admin/moderator, or an algorithmic detection? Without a clear definition of how a solution gets "flagged
  unconventional", this badge cannot be consistently awarded, leading to a logic gap.
```

Here's a review of the provided roadmap document from a software architect's perspective, focusing on readability,
design, duplication, and clean architecture implications.

---

### Code Readability

While this is a markdown document, its structure and clarity directly impact how well the "code" (specifications) is
understood.

* **Clarity and Structure:** The document is exceptionally well-structured. The use of clear headings, tables, and ASCII
  art significantly enhances readability. Each spec has a dedicated section, making it easy to navigate and understand
  individual components.
* **ASCII Art for UI Mockups:** The extensive use of ASCII art for UI mockups is a strong point. It provides a clear,
  visual representation of the intended design without requiring external image assets, which is excellent for a
  text-based roadmap. However, some complex diagrams (e.g., Navbar dropdown) can be a bit dense and might benefit from a
  brief textual summary of key elements.
* **Technical Notes:** Including "Tech Notes" and "GSAP Kotlin/JS Integration" snippets directly in the relevant spec
  sections is very helpful. It bridges the gap between high-level requirements and implementation details, providing
  immediate context for developers.

### Design Issues

* **Role Model vs. Access Control Matrix Naming:** There's a minor inconsistency in role naming. The "Role Model"
  section uses `PARTICIPANT`, `EXAM_CREATOR`, `ADMIN` (uppercase, underscore-separated), while the "Access Control
  Matrix" uses `Participant`, `Exam Creator`, `Admin` (title case). While the intent is clear, standardizing these
  names (e.g., always using the `UPPER_SNAKE_CASE` for internal role identifiers and `Title Case` for display) would
  improve consistency and reduce potential confusion in code or further documentation.
* **Exam Type Naming - "Injection / Project":** The "Exam Types" section states: "> **Injection** and **Project** are
  the same type — project template exams use the same hidden-test-case injection mechanism. There is no separate "
  Project" type in the data model." This is then followed by the type `INJECTION` and the description "Injection /
  Project".
    * **Recommendation:** If "Project" is merely a specific *application* or *flavor* of an `INJECTION` type, consider
      simplifying the type name to just `INJECTION` and clarifying its dual use in the description. The current "
      Injection / Project" implies two distinct types that are then immediately clarified as one, which can be slightly
      confusing. For instance, the type could be `INJECTION` and the description "Hidden test cases injected at grade
      time; supports both standalone Kotlin and Android project templates." This would align better with the statement "
      There is no separate 'Project' type in the data model."
* **Global Components Definition:** Clearly defining "Global Components" like Navbar and Footer as "implemented once,
  used everywhere" is a good design practice, promoting reusability and consistency across the application.
* **Badge Engine Rules Location:** Explicitly stating that "Badge Engine rules" are "server-side, in `server:domain`" is
  excellent. This clearly places business logic within the appropriate layer, aligning with clean architecture
  principles.

### Duplication

* **Information Repetition (Intentional):** There is some repetition of information (e.g., exam types, leaderboard
  presence, "Create Exam" button) across different sections (e.g., "Exam Types" table, "Landing Page" sections, "Navbar"
  description). This is generally acceptable and often beneficial in a roadmap document, as it reinforces key concepts
  and shows how they manifest in different parts of the product. It's not "duplication" in the sense of redundant code
  or conflicting information, but rather a re-presentation for different contexts.

### Clean Architecture Violations

As a documentation file, `roadmap.md` cannot directly violate clean architecture. However, it can describe or imply
architectural decisions.

* **Adherence to Clean Architecture Principles:**
    * **Domain Layer Clarity:** The explicit mention of "Badge Engine rules (server-side, in `server:domain`)" is a
      strong indicator of adherence to clean architecture, placing core business rules in the domain layer, independent
      of UI or database details.
    * **Separation of Concerns:** The breakdown into distinct specs (Auth, Exam Browser, Contest Taking, Sandbox
      Execution, Exam Creation, etc.) implies a modular design where different features are encapsulated. This aligns
      well with the principle of separation of concerns central to clean architecture.
    * **Infrastructure Details:** The "GSAP Kotlin/JS Integration" section correctly treats the animation library as an
      infrastructure detail, showing how it's integrated at the technical level without dictating core business logic.
      The `setupHeroAnimation` function would naturally reside in the presentation layer, consuming domain data if
      needed.

* **Potential for Future Scrutiny:** While the document itself is good, the detailed UI mockups and animation
  descriptions for the landing page (Spec 003) highlight the importance of ensuring that the actual implementation keeps
  presentation logic separate from application and domain logic. The current document implies this separation without
  explicitly detailing it for every component, which is appropriate for a roadmap.

---

No issues of the requested types (NullPointerException risks, coroutine misuse, logic bugs, state inconsistency) were
found in the provided code context. The context consists of dependency configuration files (TOML, Gradle Kotlin DSL,
Yarn lock files) and Markdown documentation, none of which contain executable Kotlin code where these types of runtime
issues could occur.

The changes introduce new dependencies and plugins, and two new specification documents.

### `gradle/libs.versions.toml`, `kotlin-js-store/wasm/yarn.lock`, `kotlin-js-store/yarn.lock`,
`server/app/build.gradle.kts`

* **Design Issues**: The addition of `koin-plugin` and `koin-compiler` indicates the adoption of Koin for dependency
  injection. While Koin itself is a valid tool, its usage needs careful consideration to avoid Clean Architecture
  violations. If Koin is used to inject concrete implementations across architectural layers (e.g., injecting a `data`
  layer implementation directly into a `domain` layer), it would violate the Dependency Rule. However, merely adding the
  plugin does not constitute a violation; the actual implementation will determine adherence.
* **Readability**: All changes are standard for their respective file formats and maintain good readability.
* **Duplication**: No duplication is introduced.

### `specs/003-landing-page/checklists/requirements.md`

* **Design Issues**: This is an excellent addition. Creating a specification quality checklist is a strong design
  practice that promotes clear, testable, and implementation-agnostic requirements. This directly supports good
  architectural design by ensuring a solid foundation before development begins.
* **Readability**: The document is well-structured, clear, and easy to understand.
* **Duplication**: No duplication is introduced.
* **Clean Architecture Violations**: This document reinforces principles aligned with Clean Architecture by emphasizing
  technology-agnostic requirements and avoiding implementation details in specifications.

### `specs/003-landing-page/contracts/landing-stats.md`

* **Design Issues**:
    * **API Contract Definition**: Defining API contracts upfront is a robust design practice. It establishes clear
      boundaries for external interfaces, which is fundamental to Clean Architecture. The use of a unified
      `{data, meta}` envelope and consistent error responses are good patterns for API design.
    * **"v1 Implementation Note"**: While pragmatic for iterative development, including an implementation detail ("v1
      returns hardcoded values") directly within an API contract document is a slight deviation from the ideal of a
      purely abstract, implementation-agnostic contract. Contracts should ideally describe the *what*, not the *how*.
      This note could be better placed in an implementation plan or a comment in the code itself, rather than the
      contract. However, its explicit labeling as an "Implementation Note" mitigates this concern by signaling its
      temporary nature.
* **Readability**: The contract is exceptionally well-written, using clear markdown, JSON examples, and tables to convey
  information effectively.
* **Duplication**: No duplication is introduced.
* **Clean Architecture Violations**: The contract itself defines an interface, which is a core concept in Clean
  Architecture. The "v1 Implementation Note" is a minor point regarding the purity of the contract document, but it
  doesn't represent a violation of the architectural principles in the application code itself.

No issues of the specified types (NullPointerException risks, coroutine misuse, logic bugs, state inconsistency) were
found in the provided data model specification.

The document demonstrates careful consideration for:

- **Nullability**: Explicitly using `String?` where values can be null (e.g., `avatarUrl`, `UiState.Error.message`) and
  describing fallback mechanisms.
- **Edge Cases**: Handling division by zero in `computeDifficulty` by returning a neutral default for
  `attemptCount == 0`.
- **State Management**: Using a standard `UiState` sealed class for asynchronous data and defining clear UI rendering
  rules for `Loading`, `Success`, and `Error` states.
- **Data Integrity**: Specifying validation rules for `BadgeDefinition` fields to be enforced in `init` blocks.

Here's an architectural review of the provided data model specification:

---

### 1. Code Readability

* **Overall Document Clarity**: The markdown document is exceptionally well-structured, clear, and easy to read. The use
  of tables, code blocks, and descriptive notes for each entity is excellent. Cross-references (e.g., "see §8") are very
  helpful.
* **Kotlin Snippets**: The provided Kotlin code for `LandingStatsResponse`, `DifficultyTier`, `UiState`, and
  `LandingUiState` is clean, idiomatic, and easy to understand.
* **Naming**: Entity and field names are descriptive and consistent.

### 2. Design Issues

* **`LandingStatsResponse` vs. `LandingStats` (Duplication/Over-engineering)**:
    * **Issue**: `LandingStats` is a direct, 1:1 mirror of `LandingStatsResponse`. The justification "Keeps webapp
      decoupled from core models" is a common principle, but for such simple data structures (three `Int` fields) and
      given that `LandingStatsResponse` is already in `core:models` (which `webapp` presumably depends on), this
      introduces boilerplate without significant immediate benefit.
    * **Impact**: Adds an extra type to maintain, an extra mapping step (even if trivial), and increases cognitive load
      for developers to understand why two identical types exist.
    * **Recommendation**: Re-evaluate the necessity of `LandingStats`. If `LandingStatsResponse` is truly a shared model
      in `core:models` and `webapp` already depends on `core:models`, the `webapp` can directly consume
      `LandingStatsResponse`. The "decoupling" benefit is minimal here unless `LandingStatsResponse` is expected to
      diverge significantly in structure from the frontend's needs, or if `core:models` is intended to be a backend-only
      module (which its name `core:models` and KMP shared tag contradict).
* **`DifficultyTier` (Excellent Design)**:
    * **Praise**: The decision to compute `DifficultyTier` dynamically from `attemptCount` and `successCount` is an
      excellent design choice. It ensures the difficulty rating is data-driven, current, and avoids the need for manual
      updates or complex migrations. Embedding `display` and `colorClass` directly in the enum is also very practical
      for UI consistency.
* **`SessionState` (Robust Design)**:
    * The sealed class approach for `SessionState` (Guest vs. Authenticated) is a robust and standard pattern for
      managing user session across the application. The inclusion of `avatarUrl` with a clear fallback strategy and
      stable color derivation for initials is a thoughtful UX detail.
* **`UiState` (Reusable Pattern)**:
    * The generic `UiState` sealed class (`Loading`, `Success`, `Error`) is a highly reusable and effective pattern for
      managing asynchronous data loading states in the UI. This promotes consistency and reduces boilerplate across
      different parts of the application.
* **Badge Definition Validation**: The explicit validation rules for `id` and `iconPath` in `BadgeDefinition` are a good
  security and data integrity measure, preventing issues like path traversal.

### 3. Duplication

* **`LandingStatsResponse` vs. `LandingStats`**: As detailed above, this is the primary instance of direct duplication.
  While sometimes justified by architectural patterns, its utility for a simple 1:1 mapping of primitive types should be
  critically assessed.

### 4. Clean Architecture Violations

* **Module Placement**:
    * `LandingStatsResponse`, `Tier`, `ExamType`, `DifficultyTier` in `core:models` is appropriate for shared domain
      models.
    * `SessionState` and `UiState` in `app:shared` is also appropriate for application-level shared concerns that span
      UI and potentially other layers.
* **Layering**: The distinction between API response models (`LandingStatsResponse`) and frontend view models (
  `LandingStats`, `LeaderboardEntry`, `ProblemSummary`) is generally in line with clean architecture principles, aiming
  to keep UI layers decoupled from raw API contracts.
    * **Potential Over-adherence**: The `LandingStats` duplication, while adhering to a strict interpretation of "
      separate models for separate layers," might be an *over-adherence* in this specific simple case, potentially
      violating the DRY principle without sufficient gain. If `core:models` is truly a shared domain layer, then direct
      consumption of `LandingStatsResponse` by the `webapp` for simple data is not a violation.
* **Dependencies**: The document implies appropriate dependencies (e.g., `webapp` depending on `core:models` and
  `app:shared`). No explicit violations are evident from the provided context.

---

**Summary Recommendation:**

The data model specification is generally well-designed, clear, and adheres to good architectural principles. The most
significant point for re-evaluation is the direct duplication of `LandingStatsResponse` into `LandingStats`. For such a
simple data structure, consider whether the "decoupling" benefit outweighs the added boilerplate and maintenance
overhead, especially if `webapp` already depends on `core:models` for other shared entities. If `LandingStatsResponse`
is truly a shared KMP model, direct usage by the frontend is often acceptable for simple, stable data.

Here are the identified issues based on the provided implementation plan:

```markdown
- file: specs/003-landing-page/plan.md
  line: 146
  issue: State Inconsistency
  suggestion: The plan defines a generic `UiState<T>` with `UiState.Loading`, `UiState.Success(data: T)`, and
  `UiState.Error(message: String?)`. However, the `LandingUiState` data class in the diagram (and referenced in
  `HeroSection` snippet and `LandingViewModelTest` examples) uses a bespoke `StatsState` sealed class (
  `StatsState.Loading`, `StatsState.Loaded(stats: LandingStats)`, `StatsState.Error`). This creates an inconsistency in
  the state model. The `Concrete LandingViewModel` snippet correctly uses `UiState.Success` and
  `UiState.Error(e.message)`, but the diagram and other references do not.
  **Suggestion**: Unify the state model. Either `LandingUiState` should directly use `UiState<LandingStats>` (e.g.,
  `val statsState: UiState<LandingStats> = UiState.Loading`), or the `StatsState` sealed class should be explicitly
  defined as the specific implementation of `UiState` for `LandingStats`, ensuring consistency across all documentation
  and code examples. The current approach mixes two different patterns.

- file: specs/003-landing-page/plan.md
  line: 151
  issue: State Inconsistency
  suggestion: The `LandingViewModel`'s `init` block in the initial MVI diagram uses `StatsState.Error` (an object),
  which lacks error details. This contradicts the later-defined generic `UiState.Error(val message: String? = null)`.
  While the "Concrete LandingViewModel" snippet later in the document corrects this to `UiState.Error(e.message)`, the
  diagram and `HeroSection` still refer to the less informative `StatsState.Error`.
  **Suggestion**: Ensure all references to error states, especially in diagrams and code examples, consistently use the
  more detailed `UiState.Error(message: String?)` pattern. Update the diagram and `HeroSection` snippet to reflect this.

- file: specs/003-landing-page/plan.md
  line: 251
  issue: Coroutine Misuse / Logic Bug
  suggestion: The `LaunchedEffect(Unit)` in `PageTransition.kt` runs once after the initial composition. The
  `containerRef.value` is set via `ref = containerRef` during composition. There's a potential race condition or timing
  issue where `containerRef.value` might still be `null` when `LaunchedEffect(Unit)` first executes, causing the
  entrance animation to be skipped. Kilua's `ref` updates might not be synchronously available within the same
  composition frame as `LaunchedEffect(Unit)`'s initial run.
  **Suggestion**: Key the `LaunchedEffect` on `containerRef.value` itself (`LaunchedEffect(containerRef.value)`) to
  ensure the animation is triggered only when the `HTMLElement` reference is available. Alternatively, use a
  `DisposableEffect` that sets up the animation when `containerRef.value` becomes non-null and cleans it up on dispose.

- file: specs/003-landing-page/plan.md
  line: 299
  issue: Logic Bug / Constraint Violation
  suggestion: The plan explicitly states `innerHTML / outerHTML strictly forbidden (§A4 / security constitution §5)`.
  However, the `Leaderboard Counter Roll-Up` animation explicitly uses
  `Gsap.to(scoreEl, js("""{ innerHTML: 9840, snap: "innerHTML", ... }"""))`. This is a direct violation of a critical
  security and architectural constraint.
  **Suggestion**: Refactor the counter roll-up animation to update a Kilua `Text` component's content directly. GSAP can
  animate a numeric value, and the `onUpdate` callback can then update the Kilua component's text property using the
  formatted number, avoiding `innerHTML`. The same applies to `Hero Stats Counter Roll-Up`.

- file: specs/003-landing-page/plan.md
  line: 310
  issue: Logic Bug / Constraint Violation
  suggestion: Similar to the leaderboard counter, the `Hero Stats Counter Roll-Up` also explicitly uses `innerHTML`,
  directly violating the `innerHTML / outerHTML strictly forbidden` constraint.
  **Suggestion**: Refactor this animation to update a Kilua `Text` component's content directly using the `onUpdate`
  callback, avoiding `innerHTML`.

- file: specs/003-landing-page/plan.md
  line: 374
  issue: State Inconsistency
  suggestion: The plan states that `Navbar reads JWT session state (already in ThemeManager/LocaleManager pattern)` but
  also introduces `session/SessionState.kt` as a NEW file. This creates ambiguity regarding the canonical source and
  management of session state for the `GlobalNavBar`. If `SessionState.kt` is the new, preferred approach, the reference
  to `ThemeManager/LocaleManager` handling JWT session state should be clarified or removed to prevent redundant or
  conflicting state management logic.
  **Suggestion**: Clarify how `session/SessionState.kt` integrates with or replaces the existing
  `ThemeManager/LocaleManager` pattern for JWT session state. Ensure a single, consistent source of truth for session
  information.

- file: specs/003-landing-page/plan.md
  line: 418
  issue: Logic Bug
  suggestion: The `BadgeDefinitionTest` description states it verifies `iconClass regex`. However, the "Custom Badge &
  Cup Icons" section later in the plan explicitly states that `BadgeDefinition` will change to use `iconPath: String`
  instead of `iconClass: String`, with a new `iconPath` regex validation.
  **Suggestion**: Update the `BadgeDefinitionTest` description to reflect the correct validation for `iconPath` (e.g.,
  `iconPath regex`) to align with the planned implementation.
```

The implementation plan is well-structured and demonstrates a strong understanding of clean architecture principles,
multiplatform development, and modern frontend practices. The level of detail, especially in animation design and
testing strategy, is commendable.

Here are the observations focusing on code readability, design issues, duplication, and clean architecture violations:

---

### General Observations

* **Overall Design & Readability**: The plan is exceptionally clear and well-documented. The MVI diagram, explanations
  for design choices (e.g., domain model in repository, partial loading, generic `UiState`), and detailed animation
  breakdown significantly enhance readability and understanding for anyone implementing or reviewing the code.
* **Clean Architecture Adherence**: The plan explicitly follows MVI and clean architecture principles, particularly with
  the separation of concerns between View, ViewModel, and Repository, and the use of domain models. This is a strong
  foundation.
* **Duplication Avoidance**: The plan actively addresses potential duplication through:
    * A generic `UiState<T>` sealed class.
    * A custom multiplatform `ViewModel` base class.
    * `expect`/`actual` for `ioDispatcher`.
    * Reusable layout components (`GlobalNavBar`, `Footer`, `PageTransition`).
    * GSAP wrappers (`GsapInterop.kt`) to avoid repetitive raw JS calls.

---

### Specific Points

1. **`ViewModel` Base Class and `Dispatchers.Main.immediate`**:
    * **Design Issue (Minor / Potential Misunderstanding)**: While `Dispatchers.Main.immediate` is generally good for UI
      responsiveness, its use in the *base* `ViewModel`'s `viewModelScope` might be slightly aggressive for *all*
      ViewModel operations. `Dispatchers.Main` (without `immediate`) is often sufficient, as `immediate` can sometimes
      lead to unexpected re-entrancy issues if not carefully managed, especially when state updates trigger side effects
      that then try to update state again synchronously. For a base `ViewModel`, `Dispatchers.Main` is a safer default,
      allowing specific operations to opt into `immediate` if truly necessary for performance-critical UI updates.
    * **Recommendation**: Consider if `Dispatchers.Main` (non-immediate) would be a more robust default for the base
      `ViewModel` scope, reserving `immediate` for specific, performance-critical UI updates where re-entrancy is
      explicitly handled or known not to be an issue. This is a minor point, and `immediate` can work, but it's a common
      area for subtle bugs.

2. **`UiState` Generic Pattern**:
    * **Duplication / Readability (Excellent)**: The introduction of `sealed class UiState<out T>` is an excellent
      design choice. It prevents boilerplate `Loading`, `Success`, `Error` states from being redefined for every
      feature, significantly improving readability and reducing duplication across the application. This is a strong
      positive.

3. **GSAP Interop (`@JsModule`)**:
    * **Readability / Design (Excellent)**: The decision to use `@JsModule` and wrap all GSAP calls in typed Kotlin
      functions (`GsapInterop.kt`) is crucial for maintaining a Kotlin-first codebase. It vastly improves readability,
      type safety, and maintainability compared to embedding raw `js("...")` calls directly in components. This aligns
      perfectly with the "Kotlin-First Stack" principle.

4. **Repository Returning Domain Model**:
    * **Clean Architecture (Excellent)**: The explicit design decision for `LandingStatsRepository` to return a domain
      model (`LandingStats`) rather than an API response or UI model is a textbook example of adhering to the Dependency
      Inversion Principle and maintaining a clean architecture. It ensures the data layer is independent of UI concerns.

5. **Partial Loading Strategy**:
    * **Design Issue (None, Excellent UX)**: The decision to only show skeleton/loading states for the dynamic stats
      counters and render static content immediately is a great user experience choice. It avoids unnecessary blocking
      and improves perceived performance, which is a good design consideration for a landing page.

6. **`BadgeDefinition` Evolution (`iconClass` to `iconPath`)**:
    * **Design Issue (None, Excellent Improvement)**: The planned change from `iconClass: String` (implying FontAwesome
      or similar) to `iconPath: String` for custom SVG assets is a significant design improvement. It reduces external
      dependency, allows for unique branding, and provides more control over assets. This is a very positive change.

7. **Animation Design Detail**:
    * **Readability / Design (Excellent)**: The detailed breakdown of each animation, including GSAP configurations,
      durations, and easing, is highly beneficial. It ensures consistency and provides clear instructions for
      implementation, which will lead to more readable and maintainable animation code. The note about
      `CognitiveComplexMethod` for animation code is also a good proactive measure.

8. **Koin Module Scoping (`single` vs `factory`)**:
    * **Design (Good)**: The distinction between `single<HttpClient>` (shared instance) and
      `factory<LandingViewModel>` (fresh instance per mount) demonstrates appropriate dependency scoping, which is a
      good practice for managing resources and state in an SPA.

9. **`PageTransition.kt`**:
    * **Design (Good)**: Encapsulating page transitions in a reusable `PageTransition` component is a clean way to apply
      consistent animations across different routes without duplicating logic.

---

### Conclusion

This implementation plan is of very high quality. It proactively addresses common pitfalls related to architecture,
maintainability, and user experience. The explicit focus on clean architecture, generic patterns to reduce duplication,
and detailed design choices for animations and UI states makes this a robust and well-thought-out plan. The minor point
about `Dispatchers.Main.immediate` is a nuance rather than a flaw, and the overall approach is exemplary.

The provided content is a Markdown documentation file (`quickstart.md`), not Kotlin source code. Therefore, it is not
possible to identify NullPointerException risks, coroutine misuse, logic bugs, or state inconsistency within this
document. These types of issues are specific to executable code.

This `quickstart.md` file is well-structured and clear. It effectively guides a developer through setting up and running
the landing page and its associated backend.

### Design Issues

* **None significant.** The document follows a logical flow from prerequisites to running components and testing. The
  inclusion of specific testing scenarios (e.g., reduced-motion) is a good design choice for a quickstart.

### Duplication

* **Minor repetition of `CHROME_BIN="..."`**: The `CHROME_BIN="..."` prefix is repeated for each `jsBrowserTest`
  command. While it's good to show the full command for copy-pasting, it could be slightly more concise.
    * **Suggestion**: Since `CHROME_BIN` is already mentioned in the "Prerequisites" section as an environment variable
      to be set, the test commands could potentially omit the explicit `CHROME_BIN="..."` prefix if the expectation is
      that the user has already set it globally or in `local.properties`. However, for a quickstart, showing the full
      command is often preferred for clarity, so this is a very minor point.

### Code Readability (of commands/instructions)

* **Excellent**: All commands are clearly presented in code blocks, and the instructions are straightforward and easy to
  follow. The explanations for the Vite proxy and the backend stats endpoint are particularly helpful.

### Clean Architecture Violations

* **Not applicable**: This document is a quickstart guide, not source code. It adheres to good documentation practices
  by focusing on operational steps rather than deep architectural details, which is appropriate for its purpose.

Here are the findings based on the provided research document:

```markdown
- file: specs/003-landing-page/research.md
  line: 104
  issue: Logic Bug / Redundancy
  suggestion: The `gsap.call(() → restartFromPhaseA())` at the end of Phase B appears redundant if the wrapper timeline
  is configured with `repeat: -1`. A timeline with `repeat: -1` will automatically restart from the beginning after its
  duration. Calling a function to explicitly restart it might lead to unexpected behavior, double restarts, or conflicts
  with the native looping mechanism. Consider removing this explicit call if the wrapper timeline's `repeat: -1` is
  intended to handle the looping.

- file: specs/003-landing-page/research.md
  line: 104
  issue: State Inconsistency
  suggestion: The interaction between the main looping timeline (with `repeat: -1`) and the "Manual tab switch" logic
  needs careful consideration. If a manual tab switch "kills current timeline" and "starts the appropriate phase's
  sub-timeline," it's crucial to define what happens when that manually started sub-timeline finishes. Does it rejoin
  the main loop, or does the main loop remain killed? Ensure that manual interventions gracefully manage the state of
  the main looping animation to prevent conflicts or unexpected behavior (e.g., the main loop restarting unexpectedly
  after a manual switch completes).

- file: specs/003-landing-page/research.md
  line: 147
  issue: NullPointerException risk
  suggestion: In the frontend Ktor Client call, `client.get(...).body<ApiEnvelope<LandingStatsResponse>>().data`
  directly accesses the `data` property. While `buildSuccessEnvelope` on the backend implies `data` will always be
  present for a successful response, the `ApiEnvelope` model itself (not shown here) might define `data` as nullable (
  `data: T?`) to accommodate error responses or other scenarios. If `data` can be `null` at runtime (e.g., due to an
  unexpected API response format or an error not caught by `body()`), accessing `.data` directly would result in a
  `NullPointerException`. It's safer to either ensure `ApiEnvelope.data` is non-nullable for success cases or handle
  potential nullability on the client side (e.g., using safe calls `?.` or explicit null checks).
```

The research document outlines a well-considered approach to the landing page feature, demonstrating a strong
understanding of clean architecture principles and pragmatic incremental development.

Here are the observations based on the specified focus areas:

### R-001 — GSAP 3.15.0 Kotlin/JS Interop

* **Code Readability / Design Issues**:
    * The decision to use `@JsModule` external object declarations and route all GSAP API calls through Kotlin wrappers
      is excellent for code readability and maintainability, preventing raw `js(...)` strings from polluting component
      code.
    * However, the provided `GsapInterop.kt` snippet still uses `dynamic` for `targets` and `vars`. While `dynamic` is
      necessary for direct JS interop, the statement "All GSAP API calls go through *typed* Kotlin wrappers" implies a
      higher level of type safety than `dynamic` provides. For common GSAP calls (e.g., `to`, `from`), consider defining
      Kotlin data classes or interfaces that mirror the expected JavaScript `vars` objects. This would provide
      compile-time safety, better IDE auto-completion, and clearer intent for the animation properties, moving beyond
      just wrapping the `external` declarations.
    * Example: Instead of `fun to(targets: dynamic, vars: dynamic): dynamic`, one could have a wrapper like
      `fun animateTo(targets: Element, properties: GsapToProperties): Timeline` where `GsapToProperties` is a Kotlin
      data class.

### R-003 — Hero Animation Sequence (Scripted)

* **Design Issues**:
    * The "Manual tab switch" mechanism ("`onClick` kills current timeline, `gsap.set()` resets elements, then starts
      the appropriate phase's sub-timeline") is a common and acceptable pattern for UI-specific animations. For a
      landing page hero, this direct manipulation of the animation library is pragmatic. If the animation logic were to
      become significantly more complex or require reuse across different parts of the application, encapsulating this
      logic within a dedicated `AnimationController` or `HeroAnimationService` could further improve separation of
      concerns and testability. For the current scope, it's a reasonable approach.

### R-004 — Landing Stats API

* **Design Issues / Clean Architecture Violations**:
    * The use of hardcoded values in the backend route for v1 is explicitly noted as temporary, with a clear plan for
      replacement in later specs. This is a pragmatic decision for incremental development and does not violate clean
      architecture as long as the higher layers (application, domain) depend on an abstraction (e.g.,
      `LandingStatsRepository`) rather than the concrete, hardcoded implementation.
    * The `toLandingStats()` extension function on the frontend, mapping `ApiEnvelope<LandingStatsResponse>` to
      `LandingStats`, is a good practice. It ensures that the UI/application layer works with a domain-specific model (
      `LandingStats`) rather than the raw API DTO (`LandingStatsResponse`), adhering to the separation of concerns
      inherent in clean architecture.

### R-005 — Placeholder Data Strategy

* **Design Issues / Clean Architecture Violations**:
    * The strategy of using static Kotlin objects for placeholder data in v1, combined with the explicit statement
      that "These objects are replaced with API calls in specs 005 and 010 with zero changes to the section components (
      the ViewModel injects the data source)," is an excellent example of adhering to clean architecture and dependency
      inversion. It ensures that UI components and ViewModels depend on abstractions (e.g., `ProblemRepository`,
      `LeaderboardRepository` interfaces), allowing the underlying data source implementation (placeholder vs. real API)
      to be swapped without affecting the presentation layer. This is a strong design choice for maintainability and
      testability.

### R-006 — Auth Session Interface (Stub for Landing Page)

* **Design Issues / Clean Architecture Violations**:
    * Defining `SessionState` as a `sealed class` in `app:shared` and providing a `StubUserSession` (guest) for v1, with
      a clear plan for replacement, is another exemplary application of clean architecture principles. The
      `GlobalNavBar` receiving `SessionState` as a parameter and the `LandingViewModel` exposing it from a
      `SessionRepository` ensures that the UI and ViewModel layers depend on an abstract `SessionState` and
      `SessionRepository` interface, rather than a concrete authentication implementation. This promotes loose coupling
      and makes the system highly adaptable to future changes in authentication mechanisms.

### R-007 — Badge Definitions (Static Catalogue)

* **Design Issues**:
    * Using static constants for badge definitions is appropriate given they are described as a "Static Catalogue" and
      not expected to change dynamically or require API calls for v1. This is a pragmatic and efficient way to manage
      such data. If these definitions were to become dynamic or configurable in the future, this decision would need to
      be revisited, potentially moving them to a database and an API endpoint. For the current scope, it's a sound
      decision.

---

**Overall Summary**:
The document demonstrates a mature and thoughtful approach to feature development, prioritizing clean architecture,
separation of concerns, and pragmatic incremental delivery. The use of stubs and placeholders, coupled with clear plans
for their replacement, is particularly commendable. The only minor area for potential improvement is to enhance the type
safety of the GSAP Kotlin wrappers beyond `dynamic` for better compile-time checks and developer experience.

The provided content is a feature specification in Markdown format (`spec.md`), not Kotlin code.

Therefore, I cannot identify:

- NullPointerException risks
- Coroutine misuse
- Logic bugs
- State inconsistency

Please provide Kotlin code for analysis.

The specification is well-structured, comprehensive, and clearly articulates user stories, requirements, and success
criteria. This greatly aids readability and understanding. However, as a senior architect, I've identified a few areas
related to design, potential duplication, and clean architecture principles that warrant attention.

---

### Code Readability (Specification Clarity & Structure)

* **Positive:** The overall structure, use of headings, and consistent formatting (e.g., "Given/When/Then" for
  acceptance scenarios) are excellent. The separation into User Scenarios, Requirements, Key Entities, Success Criteria,
  and Assumptions is highly effective.
* **Minor Improvement - Requirement Numbering:**
    * **Duplication:** There are two requirements numbered `FR-008`. One under "Navbar" and another under "Hero
      Section". This should be corrected to ensure unique identifiers for all functional requirements (e.g., `FR-008`
      and `FR-009` for the hero section, or renumbering the hero section requirements starting from `FR-009`). This is
      crucial for clear referencing and tracking.

---

### Design Issues

1. **Hero Section Animation Complexity (FR-009, FR-010, FR-011, FR-012, SC-002, SC-003):**
    * **Issue:** The detailed, scripted, character-by-character animation with automatic tab switching and continuous
      looping is a significant frontend engineering challenge.
    * **Design Concern:**
        * **Performance:** Achieving a Lighthouse Performance score of "≥ 80 on desktop and ≥ 70 on mobile" while
          running such a complex, continuous animation is ambitious. The requirement that "animations do not degrade the
          score by more than 10 points" acknowledges the risk but doesn't mitigate the inherent complexity. This could
          easily become a performance bottleneck.
        * **Maintainability:** Scripting and maintaining two distinct, timed, character-by-character animations with
          state transitions (Kotlin -> Android -> loop) will be brittle. Any change to the "demo code" or "test results"
          will require careful re-scripting and re-timing.
        * **User Experience:** While initially engaging, a continuously looping, complex animation might become
          distracting or overwhelming for some users, especially if they spend time reading other parts of the hero
          section.
    * **Recommendation:** Acknowledge the high engineering cost and potential performance trade-offs more explicitly.
      Consider if the full complexity is strictly necessary for the "within seconds, they understand" goal. Alternatives
      could include a shorter, single-language demo that loops less frequently, or a high-quality video with a play
      button. If the current design is critical, allocate significant development and QA time for its implementation and
      performance tuning.

2. **Navbar State Management & Initial Load (SC-008):**
    * **Issue:** The requirement "no flash of incorrect state (e.g., signed-in UI shown briefly for a guest)" for the
      navbar is critical but challenging for client-side rendered applications.
    * **Design Concern:** If the application is purely client-side rendered (CSR), there will always be a brief period
      where the JavaScript loads, fetches authentication state, and then renders the correct UI. This "flash" is a
      common problem.
    * **Recommendation:** Explicitly consider the rendering strategy (e.g., Server-Side Rendering (SSR), Static Site
      Generation (SSG) with hydration, or a robust client-side loading pattern) for the initial page load to meet
      `SC-008`. If CSR is chosen, acknowledge the difficulty and plan for techniques like skeleton loaders or
      pre-rendering critical UI elements to minimize perceived latency.

3. **Leaderboard Preview - "You" Row Data Fetching (FR-024, Assumptions):**
    * **Issue:** The requirement for a highlighted "you" row for logged-in users, with the assumption that it "requires
      only the viewer's rank and basic stats — not a full leaderboard query — and is fetched from the same auth session
      that powers the navbar."
    * **Design Concern:** This implies a specific backend API design. It's an excellent optimization to avoid fetching
      the entire leaderboard just for one user's rank. However, the backend needs to efficiently provide both the top N
      users and the current user's specific rank/stats (which might not be in the top N) in a single, optimized call or
      two highly efficient, coordinated calls.
    * **Recommendation:** Confirm the backend API design for this. It should ideally be a single endpoint that can
      return both the top N entries and, if an authenticated user is present, their specific entry, regardless of their
      rank. This prevents N+1 issues or redundant data fetching.

---

### Duplication

* **FR-008:** As noted above, the duplicate `FR-008` identifier is a clear duplication and needs to be resolved.

---

### Clean Architecture Violations (Potential in Implementation)

While this is a specification, certain requirements suggest design choices that could lead to clean architecture
violations if not implemented carefully.

1. **Mixing Presentation Logic with Business Logic (Hero Animation - FR-009, FR-010):**
    * **Potential Violation:** The highly detailed scripting of the animation (character-by-character typing,
      compilation steps, test results, automatic tab switching, looping) is deeply embedded in the *presentation layer*.
      While the spec states "no real code is executed," the complexity of *simulating* this workflow could lead to
      tightly coupled presentation logic.
    * **Concern:** If the "core workflow" (write code → compile → test) ever changes its visual representation or steps,
      this highly scripted animation will be very fragile and require significant rework. It's a hard-coded "story"
      rather than a dynamic representation of core domain logic.
    * **Recommendation:** Ensure the implementation of this animation is strictly isolated to the UI layer, perhaps
      using a dedicated animation library or state machine that doesn't bleed into broader application state or domain
      logic. Treat it as a self-contained "widget" with its own internal state, separate from the application's core
      domain.

2. **Data Sourcing for Landing Page Sections (Assumptions, FR-020, FR-023):**
    * **Potential Violation:** The assumptions mention "hard-coded" badge definitions and "realistic placeholder data"
      for problems/leaderboard *for v1*, to be replaced with "live API data" later.
    * **Concern:** The transition from hard-coded/placeholder data to live API data (e.g., for `ProblemSummary`,
      `LeaderboardEntry`) needs to be handled cleanly. If the initial UI components are built tightly coupled to the
      placeholder data structure or directly to static data, refactoring to use a proper domain entity fetched from a
      repository might be harder.
    * **Recommendation:** Design the UI components for these sections (Problem Dataset, Leaderboard Preview) to accept
      data via clear interfaces or props that can be fulfilled by either static data (for v1) or a data fetching
      service (e.g., a `ProblemRepository` or `LeaderboardService`). This promotes separation of concerns between data
      presentation and data sourcing. The "Key Entities" section helps here by defining the expected data shape, which
      should be consistent regardless of the data source.

3. **Authentication State and Role-Based Access (Navbar, Exam Creation CTA):**
    * **Potential Violation:** The navbar and CTA logic depend heavily on authentication state and user roles (FR-002,
      FR-003, FR-006, FR-027).
    * **Concern:** This logic should ideally reside in an "Application Layer" or "Auth Service" that provides the
      current user's state and roles, which the UI then consumes. Direct checks against raw token data or tightly
      coupled UI components to authentication mechanisms would be a violation.
    * **Recommendation:** Ensure there's a clear `AuthService` or `UserService` that provides methods like
      `isLoggedIn()`, `hasRole('ExamCreator')`, `getUserPlan()`, etc., which the UI components then use. This keeps
      authentication and authorization logic separate from presentation concerns.

4. **Persistence of Theme/Language (FR-007):**
    * **Potential Violation:** "The selected theme (dark / light) and language (English / Persian) MUST persist across
      page navigations and browser refreshes for the current visitor."
    * **Concern:** This implies client-side storage (e.g., LocalStorage, Cookies). The mechanism for persistence should
      be encapsulated within a dedicated `SettingsService` or `PreferencesService` rather than having UI components
      directly interact with `localStorage` or `document.cookie`.
    * **Recommendation:** Define a service responsible for managing user preferences, which the UI consumes and updates.
      This centralizes preference management and keeps the UI clean.

Here are the identified risks and bugs based on the provided task descriptions:

- **File**: `specs/003-landing-page/tasks.md`
- **Line**: 22
- **Issue**: NullPointerException risk
- **Suggestion**: The `UiState.Error(message: String?)` allows `message` to be null. While this might be intentional, it
  introduces a potential `NullPointerException` if UI components consuming this state assume `message` is always
  non-null without explicit checks. Consider making `message` non-nullable and providing a default empty string or
  requiring a message, or ensure all UI consumers handle the null case explicitly.

---

- **File**: `specs/003-landing-page/tasks.md`
- **Line**: 30
- **Issue**: Logic bug (potential division by zero)
- **Suggestion**: The `computeDifficulty` function's description states `attemptCount==0` → MEDIUM. However, if the
  percentage calculations (`≥60% → EASY`, `≥25% → MEDIUM`, else `HARD`) are performed *before* checking for
  `attemptCount == 0`, it will lead to a division by zero error (`successCount / attemptCount`). Ensure the
  `attemptCount == 0` condition is the very first check in the function to prevent this. This bug would be inherited by
  T048.

---

- **File**: `specs/003-landing-page/tasks.md`
- **Line**: 35
- **Issue**: NullPointerException risk
- **Suggestion**: The `SessionState.Authenticated(..., avatarUrl: String?, ...)` allows `avatarUrl` to be null. This
  introduces a potential `NullPointerException` if UI components (e.g., `GlobalNavBar.kt` in T032) assume `avatarUrl` is
  always non-null without explicit checks or fallback logic. T032's description mentions an "initials fallback circle",
  which mitigates this, but the risk exists if not consistently applied. Ensure all consumers of `avatarUrl` explicitly
  handle its nullability.

---

- **File**: `specs/003-landing-page/tasks.md`
- **Line**: 39
- **Issue**: NullPointerException risk & Coroutine misuse
- **Suggestion**:
    1. **NPE Risk**: The call `.body<ApiEnvelope<LandingStatsResponse>>().data` in `LandingStatsRepositoryImpl` assumes
       that the `data` field within `ApiEnvelope` is never null when the API call is successful. If the `ApiEnvelope`
       can return a null `data` field (e.g., in case of an API-level error where `data` is null but an `error` field is
       populated), accessing `.data` directly would result in a `NullPointerException`. The `ApiEnvelope` structure
       should be designed to handle this, and the access should include null checks (`?.data`) or robust error handling.
    2. **Coroutine Misuse (Minor)**: Wrapping a non-blocking Ktor client call with `withContext(ioDispatcher)` when
       `ioDispatcher` is `Dispatchers.Default` (for JS/WASM targets, as per T004) is often an unnecessary context
       switch. Ktor client operations on JS/WASM are inherently non-blocking and typically run on the main event loop.
       While not a critical bug, it adds a slight overhead without providing significant benefit for I/O offloading in
       these environments. Consider if the explicit context switch is truly necessary or if the default dispatcher (
       often `Dispatchers.Main` for the continuation) would suffice.

---

- **File**: `specs/003-landing-page/tasks.md`
- **Line**: 119
- **Issue**: Logic bug (rate limiting behind proxy)
- **Suggestion**: In `Application.kt`, the `requestKey { call -> call.request.origin.remoteHost }` for rate limiting
  uses the immediate remote host. If the server is deployed behind a reverse proxy or load balancer (common in
  production), `remoteHost` will consistently be the proxy's IP address, causing all client requests to be rate-limited
  as if they originated from a single source. For production, the `requestKey` should typically extract the client's
  real IP from standard headers like `X-Forwarded-For` or `X-Real-IP`.

The provided `tasks.md` file is exceptionally well-structured and detailed, demonstrating a strong understanding of
modern software development principles, including Clean Architecture, Kotlin Multiplatform (KMP), and comprehensive
quality assurance.

Here's a breakdown focusing on the requested areas:

---

### Code Readability (of the tasks themselves)

The readability of the task list is excellent:

1. **Consistent Format:** The `- [ ] T### [P?] [US?] Description — file/path` format is clear, concise, and consistently
   applied, making it easy to scan and understand each task's ID, parallelizability, user story association,
   description, and target file.
2. **Clear Descriptions:** Task descriptions are generally precise and provide sufficient detail without being overly
   verbose. For instance, animation tasks (e.g., T021, T022) specify key GSAP properties like `stagger`, `ease`, and
   `duration`, which is helpful for implementation.
3. **Logical Phasing:** The division into phases (Setup, Foundational, User Stories, Polish) is intuitive and reflects a
   sensible development workflow, starting with infrastructure and models before moving to UI features and cross-cutting
   concerns.
4. **Checkpoints:** The "Checkpoint" sections after each major phase are a fantastic addition, providing clear
   validation steps and expected outcomes, which significantly aids progress tracking and quality control.
5. **Dependencies and Strategy:** The dedicated sections for "Dependencies & Execution Order" and "Implementation
   Strategy" are invaluable. They clearly articulate the relationships between phases and user stories, identify
   parallelization opportunities, and outline an MVP path. This meta-information greatly enhances the overall clarity
   and manageability of the project.

### Design Issues

The overall design implied by the tasks is robust and well-considered. Only minor potential areas for attention:

1. **Complexity of Large UI Components:** Tasks like `HeroSection.kt` (T019-T024) and `GlobalNavBar.kt` (T031-T034)
   describe components that are inherently complex, encompassing layout, multiple states (guest/authenticated, active
   tab), intricate animations, and comprehensive accessibility features.
    * **Mitigation:** This potential issue is proactively addressed by **T059**, which mandates Detekt checks and
      explicitly suggests splitting functions exceeding the `CognitiveComplexMethod` threshold. This is an excellent
      practice that will help maintain modularity and readability within these complex components.
2. **GSAP Interop Layer:** The extensive use of GSAP for animations (T002, T007, and numerous UI tasks) means a
   significant portion of the UI's dynamic behavior will rely on direct JavaScript interop. While `GsapInterop.kt` (
   T002) provides a typed facade, the animation builders themselves will be imperative and tied to this specific
   library.
    * **Design Choice:** This is a conscious design choice when opting for a powerful animation library like GSAP. The
      tasks correctly integrate `matchMedia` for reduced-motion accessibility (T007, T020, T030, T054), which is a
      crucial consideration for such animation-heavy designs.

### Duplication

There is no significant duplication of information or tasks. The document effectively uses different sections to present
the same underlying task data from various perspectives (e.g., chronological list, dependency graph, summary table),
which enhances understanding rather than creating redundancy.

### Clean Architecture Violations

The task list demonstrates **excellent adherence to Clean Architecture principles**. No violations are observed.

Here's why:

1. **Clear Separation of Concerns:**
    * **Domain Models vs. DTOs:** T009 defines `LandingStatsResponse` (a data transfer object, likely from the API),
      while T014 defines `LandingStats` (a domain model) and the `toDomainModel()` extension. This is a textbook example
      of separating external data structures from internal domain representations.
    * **Repository Pattern:** T015 correctly places the `LandingStatsRepository` interface in `app:shared` (representing
      the domain/application layer) and its `LandingStatsRepositoryImpl` in `app/webApp` (the data layer). This adheres
      to the Dependency Inversion Principle, where the domain depends on an abstraction, and the data layer implements
      it.
    * **ViewModel Responsibilities:** T003 establishes an abstract `ViewModel` base, and T016 defines `LandingViewModel`
      to manage `LandingUiState` (T016) by fetching data from the repository. This correctly positions the ViewModel as
      the orchestrator between the UI and the domain/data layers.
    * **UI State Management:** T005 introduces a generic `UiState` sealed class, a standard and effective pattern for
      representing various states of UI data (Loading, Success, Error).
2. **Dependency Rule:** The structure implies that dependencies flow inwards:
    * UI components (`LandingPage.kt`, `HeroSection.kt`) depend on `LandingViewModel`.
    * `LandingViewModel` depends on `LandingStatsRepository` (interface).
    * `LandingStatsRepositoryImpl` depends on `HttpClient` and `LandingStatsResponse` (DTO).
    * Core domain models and enums (T009-T013) are in `commonMain` or `app:shared`, forming the innermost layer.
3. **Testability:** The inclusion of specific tests for ViewModels (T016 checkpoint), repository implementations (T018),
   UI components (T017, T027), and backend routes/rate limits (T028, T029) indicates a design that prioritizes
   testability at all layers.
4. **Kotlin Multiplatform (KMP) Best Practices:**
    * **`expect`/`actual` for Dispatchers:** T004 correctly uses `expect val ioDispatcher: CoroutineDispatcher` in
      `commonMain` with platform-specific `actual` implementations, ensuring platform-agnostic coroutine management.
    * **Shared Models and Interfaces:** Core domain concepts like `DifficultyTier`, `ExamType`, `Tier`, `SessionState` (
      T010-T013) and repository interfaces (T015) are placed in `commonMain` or `app:shared`, enabling code reuse across
      platforms.
    * **Platform-Specific Implementations:** Ktor `HttpClient` (T006) and `LandingStatsRepositoryImpl` (T015) are
      correctly placed in `webApp`, as they are platform-specific data layer implementations.

---

**Conclusion:**

This `tasks.md` file is an exemplary document for guiding development. It sets a high standard for code quality,
architectural integrity, and comprehensive feature delivery. The explicit inclusion of tasks for Detekt complexity
checks, Kover coverage, and extensive accessibility/i18n audits demonstrates a mature and responsible approach to
software development.

---
Generated by Koog Review System
