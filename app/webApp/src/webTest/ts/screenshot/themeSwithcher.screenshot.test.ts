import {expect, test} from "@playwright/test";

test("theme switcher screenshots", async ({page}) => {
    await page.goto("/playground");

    // disable animations
    await page.addStyleTag({
        content: `
            *,
            *::before,
            *::after {
                transition: none !important;
                animation: none !important;
            }
        `,
    });

    // open ThemeSwitcher section
    await page.getByText("ThemeSwitcher", {
        exact: true,
    }).click();

    // ── main switcher ─────────────────────────

    const mainSwitcher = page.locator(
        "#theme-switcher-main",
    );

    await expect(mainSwitcher).toBeVisible();

    // light mode
    await expect(mainSwitcher).toHaveScreenshot(
        "theme-switcher-light.png",
    );

    // toggle dark
    await mainSwitcher.click();

    await expect(mainSwitcher).toHaveScreenshot(
        "theme-switcher-dark.png",
    );

    // toolbar screenshot in dark
    const toolbarSwitcher = page.locator(
        "#theme-switcher-toolbar",
    );

    await expect(toolbarSwitcher).toBeVisible();

    await expect(
        page.locator(".rounded-xl.border.border-outline\\/20.bg-surface-container"),
    ).toHaveScreenshot(
        "theme-switcher-toolbar-dark.png",
    );

    // back to light
    await toolbarSwitcher.click();

    await expect(mainSwitcher).toHaveScreenshot(
        "theme-switcher-back-light.png",
    );
});
