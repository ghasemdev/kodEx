import {expect, test} from "@playwright/test";

test("sidebar desktop screenshots", async ({page}) => {
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

    // open Sidebar preview
    await page.getByText("Sidebar", {
        exact: true,
    }).click();

    const sidebar = page.locator("#sidebar");

    await expect(sidebar).toBeVisible();

    // default desktop sidebar
    await expect(sidebar).toHaveScreenshot(
        "sidebar-desktop-default.png",
    );

    // selected states
    const items = [
        "dashboard",
        "exams",
        "candidates",
        "results",
        "settings",
    ];

    for (const item of items) {
        const navItem = page.locator(
            `#sidebar-item-${item}`,
        );

        await navItem.click();

        await expect(sidebar).toHaveScreenshot(
            `sidebar-desktop-${item}.png`,
        );
    }
});

test("sidebar tablet screenshots", async ({page}) => {
    // tablet viewport
    await page.setViewportSize({
        width: 900,
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

    // open Sidebar preview
    await page.getByText("Sidebar", {
        exact: true,
    }).click();

    const sidebar = page.locator("#sidebar");

    await expect(sidebar).toBeVisible();

    // collapsed rail state
    await expect(sidebar).toHaveScreenshot(
        "sidebar-tablet-collapsed.png",
    );

    // expand
    const collapseButton = page.locator(
        "#sidebar-collapse-button",
    );

    await collapseButton.click();

    // expanded state
    await expect(sidebar).toHaveScreenshot(
        "sidebar-tablet-expanded.png",
    );

    // selection states
    const items = [
        "dashboard",
        "exams",
        "candidates",
        "results",
        "settings",
    ];

    for (const item of items) {
        const navItem = page.locator(
            `#sidebar-item-${item}`,
        );

        await navItem.click();

        await expect(sidebar).toHaveScreenshot(
            `sidebar-tablet-${item}.png`,
        );
    }
});

test("sidebar hidden on mobile", async ({page}) => {
    // mobile viewport
    await page.setViewportSize({
        width: 390,
        height: 844,
    });

    await page.goto("/playground");

    // open Sidebar preview
    await page.getByText("Sidebar", {
        exact: true,
    }).click();

    // sidebar should not exist
    await expect(
        page.locator("#sidebar"),
    ).toHaveCount(0);
});
