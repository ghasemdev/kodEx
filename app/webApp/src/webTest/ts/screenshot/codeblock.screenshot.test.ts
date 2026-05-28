import {expect, test} from "@playwright/test";

test("codeblock screenshots", async ({page, context}) => {
    await context.grantPermissions([
        "clipboard-read",
        "clipboard-write",
    ]);

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

    // open CodeBlock section
    await page.getByText("CodeBlock", {
        exact: true,
    }).click();

    // kotlin block
    const kotlinBlock = page.locator(
        "#codeblock-kotlin",
    );

    await expect(kotlinBlock).toBeVisible();

    await expect(kotlinBlock).toHaveScreenshot(
        "codeblock-kotlin.png",
    );

    // copy button state
    const kotlinCopyButton = kotlinBlock.getByRole(
        "button",
    );

    await expect(kotlinCopyButton).toHaveScreenshot(
        "codeblock-copy-default.png",
    );

    await kotlinCopyButton.click();

    await expect(kotlinCopyButton).toHaveScreenshot(
        "codeblock-copy-copied.png",
    );

    // json block
    const jsonBlock = page.locator(
        "#codeblock-json",
    );

    await expect(jsonBlock).toBeVisible();

    await expect(jsonBlock).toHaveScreenshot(
        "codeblock-json.png",
    );

    // no copy block
    const noCopyBlock = page.locator(
        "#codeblock-no-copy",
    );

    await expect(noCopyBlock).toBeVisible();

    await expect(noCopyBlock).toHaveScreenshot(
        "codeblock-no-copy.png",
    );

    // ensure no copy button exists
    await expect(
        noCopyBlock.getByRole("button"),
    ).toHaveCount(0);
});
