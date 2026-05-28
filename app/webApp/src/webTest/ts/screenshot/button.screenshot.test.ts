import { test, expect } from "@playwright/test";

test("button screenshots", async ({ page }) => {
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

    // open button section
    await page.getByText("Button", { exact: true }).click();

    // variants
    const variants = [
        "primary",
        "secondary",
        "ghost",
        "danger",
        "link",
    ];

    for (const variant of variants) {
        const button = page.locator(`#button-${variant}`);

        await expect(button).toBeVisible();

        await expect(button).toHaveScreenshot(
            `button-${variant}.png`,
        );
    }

    // sizes
    const sizes = ["sm", "md", "lg"];

    for (const size of sizes) {
        const button = page.locator(`#button-${size}`);

        await expect(button).toBeVisible();

        await expect(button).toHaveScreenshot(
            `button-size-${size}.png`,
        );
    }

    // disabled
    const disabledButton = page.locator("#button-disabled");

    await expect(disabledButton).toBeVisible();

    await expect(disabledButton).toHaveScreenshot(
        "button-disabled.png",
    );
});
