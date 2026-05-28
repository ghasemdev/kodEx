import {expect, test} from "@playwright/test";

test("toast screenshots", async ({page}) => {
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

    // open Toast section
    await page.getByText("Toast", {
        exact: true,
    }).click();

    const variants = [
        {
            button: "#toast-info-button",
            screenshot: "toast-info.png",
        },
        {
            button: "#toast-success-button",
            screenshot: "toast-success.png",
        },
        {
            button: "#toast-warning-button",
            screenshot: "toast-warning.png",
        },
        {
            button: "#toast-error-button",
            screenshot: "toast-error.png",
        },
    ];

    for (const variant of variants) {
        // trigger toast
        await page.locator(variant.button).click();

        // latest toast
        const toast = page.locator(
            "#toast-container > div",
        ).last();

        await expect(toast).toBeVisible();

        // screenshot
        await expect(toast).toHaveScreenshot(
            variant.screenshot,
        );

        // close toast
        await toast.getByText("✕").click();

        await expect(toast).toHaveCount(0);
    }
});
