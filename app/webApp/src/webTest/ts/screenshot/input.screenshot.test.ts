import {expect, test} from "@playwright/test";

test("input screenshots", async ({page}) => {
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

    // open Input section
    await page.getByText("Input", {
        exact: true,
    }).click();

    // email input
    const emailInput = page.locator("#input-email");

    await expect(emailInput).toBeVisible();

    await expect(emailInput).toHaveScreenshot(
        "input-email.png",
    );

    // password input (empty)
    const passwordInput = page.locator("#input-password");

    await expect(passwordInput).toBeVisible();

    await expect(passwordInput).toHaveScreenshot(
        "input-password-empty.png",
    );

    // password input (error state)
    await passwordInput.fill("123");

    await expect(passwordInput).toHaveScreenshot(
        "input-password-error.png",
    );

    // disabled input
    const disabledInput = page.locator("#input-disabled");

    await expect(disabledInput).toBeVisible();

    await expect(disabledInput).toHaveScreenshot(
        "input-disabled.png",
    );

    // textarea
    const textArea = page.locator("#textarea-notes");

    await expect(textArea).toBeVisible();

    await expect(textArea).toHaveScreenshot(
        "textarea-empty.png",
    );

    // textarea filled
    await textArea.fill(
        "This is a multiline textarea example.",
    );

    await expect(textArea).toHaveScreenshot(
        "textarea-filled.png",
    );
});
