import {expect, test} from "@playwright/test";

test("language switcher screenshots", async ({page}) => {
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

    // open LanguageSwitcher preview
    await page.getByText("LanguageSwitcher", {
        exact: true,
    }).click();

    const switcher = page.locator(
        "#language-switcher-main",
    );

    const toggle = page.locator(
        "#language-switcher-toggle",
    );

    await expect(toggle.first()).toBeVisible();

    // default english state
    await expect(toggle.first()).toHaveScreenshot(
        "language-switcher-en.png",
    );

    // open dropdown
    await toggle.first().click();

    const dropdown = page.locator(
        "#language-switcher-dropdown",
    );

    await expect(dropdown).toBeVisible();

    await expect(dropdown).toHaveScreenshot(
        "language-switcher-dropdown.png",
    );

    // switch to persian
    await page.locator("#language-option-fa").click();

    // persian state
    await expect(toggle.first()).toHaveScreenshot(
        "language-switcher-fa.png",
    );

    // rtl screenshot
    await expect(page.locator("body")).toHaveScreenshot(
        "language-switcher-rtl-layout.png",
    );

    // reopen dropdown in persian
    await toggle.first().click();

    await expect(dropdown).toHaveScreenshot(
        "language-switcher-dropdown-fa.png",
    );

    // back to english
    await page.locator("#language-option-en").click();

    await expect(toggle.first()).toHaveScreenshot(
        "language-switcher-back-en.png",
    );
});
