# Quick-Start Guide: Design System (Feature 002)

## Prerequisites

- JDK 21, Node.js 22+, Docker Compose (for full stack)
- Kilua 0.0.34 Gradle plugin already configured in `app/webApp`

## 1. Run the Dev Playground

```bash
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

Vite dev server starts at `http://localhost:3000`. Open it in the browser.

The **playground sidebar** is visible on the left. Click any component name to see all its variants rendered in the canvas. Use the breakpoint switcher at the top of the canvas to simulate Mobile / Tablet / Desktop / TV viewports.

> The playground only appears in dev mode. Production builds contain no playground code.

## 2. Switch Language

In the running app, click the **LanguageSwitcher** in the NavBar.

- Select **English** → LTR layout, Inter font, English strings
- Select **فارسی** → RTL layout, Vazirmatn font, Persian strings

Your preference is saved in LocalStorage and restored on reload.

## 3. Switch Theme

Click the **ThemeSwitcher** (sun/moon icon) in the NavBar to cycle through Light / Dark / Auto.

## 4. Add a New Translation String

1. In your Kilua component, use `i18n.tr("Your string here")` instead of a plain string literal.
2. Run the extraction task:
   ```bash
   ./gradlew :app:webApp:gettext
   ```
   This updates `src/commonMain/resources/modules/i18n/messages.pot`.
3. Add the Persian translation to `messages-fa.po`:
   ```po
   msgid "Your string here"
   msgstr "متن فارسی شما اینجا"
   ```

## 5. Add a New Component

1. Create `app/webApp/src/commonMain/kotlin/dev/kodex/webapp/design/components/MyComponent.kt`
2. Implement as a `@Composable` function using Kilua DSL + Tailwind utility classes.
3. Register it in the playground:
   ```kotlin
   // In PlaygroundRegistry.kt (guarded by import.meta.env.DEV)
   PlaygroundEntry("MyComponent") { MyComponentPreview() }
   ```
4. It will appear immediately in the playground sidebar (hot reload via Vite HMR).

## 6. Production Build

```bash
./gradlew :app:webApp:jsBrowserProductionWebpack
```

Output in `app/webApp/build/dist/js/productionExecutable/`. Verify playground is excluded:
```bash
grep -r "PlaygroundApp" app/webApp/build/dist/js/productionExecutable/ || echo "PASS: no playground in prod"
```

## 7. Full Stack Dev (Docker Compose)

```bash
docker compose up --build
```

Frontend available at `http://localhost:3000`; API proxied to `http://localhost:8080`.
