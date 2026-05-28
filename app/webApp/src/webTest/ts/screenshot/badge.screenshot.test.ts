import {expect, test} from "@playwright/test";

test("button screenshots", async ({page}) => {
    await page.goto("http://localhost:3000/playground");

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

    // open badge section
    await page.getByText("Badge", {exact: true}).click();

    // variants
    const variants = [
        "default",
        "primary",
        "success",
        "warning",
        "danger",
        "info",
    ];

    for (const variant of variants) {
        const button = page.locator(`#badge-${variant}`);

        await expect(button).toBeVisible();

        await expect(button).toHaveScreenshot(
            `badge-${variant}.png`,
        );
    }
});
