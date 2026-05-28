import {expect, test} from "@playwright/test";

test("modal screenshots", async ({page}) => {
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

    // open Modal section
    await page.getByText("Modal", {
        exact: true,
    }).click();

    // ── info modal ─────────────────────────────

    const openInfoButton = page.locator(
        "#open-info-modal",
    );

    await expect(openInfoButton).toBeVisible();

    await expect(openInfoButton).toHaveScreenshot(
        "modal-trigger-info.png",
    );

    await openInfoButton.click();

    const infoModal = page.locator("#info-modal");

    await expect(infoModal).toBeVisible();

    await expect(infoModal).toHaveScreenshot(
        "modal-info-open.png",
    );

    // close with X button
    await infoModal.getByText("✕").click();

    await expect(infoModal).toHaveCount(0);

    // ── confirm modal ──────────────────────────

    const openConfirmButton = page.locator(
        "#open-confirm-modal",
    );

    await expect(openConfirmButton).toBeVisible();

    await expect(openConfirmButton).toHaveScreenshot(
        "modal-trigger-confirm.png",
    );

    await openConfirmButton.click();

    const confirmModal = page.locator(
        "#confirm-modal",
    );

    await expect(confirmModal).toBeVisible();

    await expect(confirmModal).toHaveScreenshot(
        "modal-confirm-open.png",
    );

    // escape close
    await page.keyboard.press("Escape");

    await expect(confirmModal).toHaveCount(0);

    // reopen for backdrop screenshot behavior
    await openConfirmButton.click();

    await expect(confirmModal).toBeVisible();

    // click backdrop
    await page.locator(".bg-black\\/50").click({
        position: {x: 10, y: 10},
    });

    await expect(confirmModal).toHaveCount(0);
});
