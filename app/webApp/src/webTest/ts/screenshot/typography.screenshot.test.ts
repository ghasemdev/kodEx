import {expect, test} from "@playwright/test";

test("typography screenshots", async ({page}) => {
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

    const sections = [
        "typography-font-families",
        "typography-type-scale",
        "typography-font-weights",
        "typography-body-text",
        "typography-code",
        "typography-color-roles",
    ];

    for (const id of sections) {
        const element = page.locator(`#${id}`);

        await expect(element).toBeVisible();

        await expect(element).toHaveScreenshot(
            `${id}.png`,
        );
    }
});
