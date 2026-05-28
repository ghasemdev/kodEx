import {expect, test} from "@playwright/test";

test("navbar desktop screenshots", async ({page}) => {
    // desktop viewport
    await page.setViewportSize({
        width: 1440,
        height: 900,
    });

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

    // open navbar preview
    await page.getByText("NavBar", {
        exact: true,
    }).click();

    const navbar = page.locator("#navbar-desktop");

    await expect(navbar).toBeVisible();

    // default desktop navbar
    await expect(navbar).toHaveScreenshot(
        "navbar-desktop-default.png",
    );

    // selected states
    const items = [
        "home",
        "exams",
        "results",
        "profile",
    ];

    for (const item of items) {
        const navItem = page.locator(
            `#navbar-item-${item}`,
        );

        await navItem.click();

        await expect(navbar).toHaveScreenshot(
            `navbar-desktop-${item}.png`,
        );
    }
});
