# Kotlin/JS + Playwright + Vitest Screenshot Testing Setup

This guide explains how to configure screenshot testing for a Kotlin/JS application using:

* Vitest
* Playwright
* html2canvas
* Kotlin/JS + Kotest

The setup keeps all screenshot bridge logic inside test source sets only, without polluting `webMain/jsMain`.

---

# 1. Install Dependencies

Inside `app/webApp`:

```shell
npm install -D vitest playwright @playwright/test html2canvas jsdom
```

Install Playwright browsers:

```shell
npx playwright install
```

---

# 2. Project Structure

```text
app/webApp/
├── playwright.config.ts
│
└── src/
    ├── webTest/
    │   └── kotlin/dev/kodex/webapp/screenshot/
    │       ├── ScreenshotBridge.kt
    │       └── ButtonScreenshotTest.kt
    │   └── ts/screenshot/dev.kodex.webapp.design.component
    │       └── button.screenshot.test.ts
    │
    └── jsTest/
        └── kotlin/dev/kodex/webapp/screenshot/
            ├── Html2Canvas.kt
            └── ScreenshotBridgeActual.kt
```

---

# 3. Configure Playwright

Create:

```text
playwright.config.ts
```

```ts
import {defineConfig} from "@playwright/test";

export default defineConfig({
    testDir: "./src/webTest/ts/screenshot/dev/kodex/webapp/design/component",

    use: {
        baseURL: "http://localhost:3000",
        headless: true,
    }
});
```

---

# 4. Kotlin Screenshot Bridge

## Common declaration

Create:

```text
src/webTest/kotlin/dev/kodex/webapp/screenshot/ScreenshotBridge.kt
```

```kotlin
package dev.kodex.webapp.screenshot

import org.w3c.dom.HTMLElement

expect suspend fun captureScreenshot(element: HTMLElement): String
```

---

# 5. html2canvas External Binding

Create:

```text
src/jsTest/kotlin/dev/kodex/webapp/screenshot/Html2Canvas.kt
```

```kotlin
@file:JsModule("html2canvas")
@file:JsNonModule

import kotlin.js.Promise
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

@JsName("default")
external fun html2canvas(element: HTMLElement): Promise<HTMLCanvasElement>
```

---

# 6. Kotlin Actual Implementation

Create:

```text
src/jsTest/kotlin/dev/kodex/webapp/screenshot/ScreenshotBridgeActual.kt
```

```kotlin
package dev.kodex.webapp.screenshot

import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement

actual suspend fun captureScreenshot(element: HTMLElement): String {
    val canvas = html2canvas(element).await()
    return canvas.toDataURL("image/png")
}
```

---

# 7. Kotest Screenshot Test

Create:

```text
src/webTest/kotlin/dev/kodex/webapp/screenshot/ButtonScreenshotTest.kt
```

```kotlin
package dev.kodex.webapp.screenshot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldStartWith
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

class ButtonScreenshotTest : FunSpec({
    test("capture screenshot") {
        val div = document.createElement("div") as HTMLDivElement

        div.innerHTML = """
            <button style="
                background: purple;
                color: white;
                padding: 12px;
                border-radius: 8px;
            ">
                Hello
            </button>
        """.trimIndent()

        document.body?.appendChild(div)

        val screenshot = captureScreenshot(div)
        screenshot.shouldStartWith("data:image/png")
    }
})
```

---

# 8. Playwright Screenshot Test

Create:

```text
src/webTest/ts/screenshot/dev/kodex/webapp/design/component/button.screenshot.test.ts
```

```ts
import { test, expect } from "@playwright/test";

test("button screenshot", async ({ page }) => {
    await page.goto("/playground");
    await page.getByText("Button", { exact: true }).click();
    const button = page.locator("#button-primary");

    await expect(button).toBeVisible();
    await expect(button).toHaveScreenshot("button-primary.png");
});
```

---

# 9. Start the Kotlin/JS App

Before running Playwright tests, start the development server.

For JS target:

```shell
./gradlew jsViteRun
```

For Wasm target:

```shell
./gradlew wasmJsViteRun
```

Expected URL:

```text
http://localhost:3000
```

---

# 10. Run Kotest Screenshot Tests

```shell
./gradlew jsBrowserTest
```

---

# 11. Run Vitest

```shell
npx vitest
```

---

# 12. Run Playwright Screenshot Tests

Open a new terminal:

```shell
npx playwright test
```

---

# 13. First Screenshot Run

On the first run, Playwright generates baseline snapshots:

```text
src/webTest/ts/screenshot/dev/kodex/webapp/design/component/button.screenshot.test.ts-snapshots/button.png
```

---

# 14. Visual Regression

On future runs, Playwright compares screenshots pixel-by-pixel.

If the UI changes unexpectedly:

```text
Error: Screenshot comparison failed
```

Generated files:

```text
button-actual.png
button-expected.png
button-diff.png
```

---

# 15. View HTML Report

```shell
npx playwright show-report
```

---

# 16. Update Snapshots

If UI changes are intentional:

```shell
npx playwright test --update-snapshots
```

---

# 17. Notes

* No screenshot bridge code exists in `webMain/jsMain`
* Production bundles remain clean
* `html2canvas` is available only in test source sets
* Vitest and Kotest share the same screenshot implementation strategy
* Compatible with Gradle-generated Vite configuration
* Easy to extend for full visual regression testing in CI/CD pipelines
