import {expect, test} from "@playwright/test";

test("card screenshots", async ({page}) => {
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

    // open Card section
    await page.getByText("Card", {
        exact: true,
    }).click();

    // basic card
    const basicCard = page.locator("#card-basic");

    await expect(basicCard).toBeVisible();

    await expect(basicCard).toHaveScreenshot(
        "card-basic.png",
    );

    // header/footer card
    const headerFooterCard = page.locator(
        "#card-header-footer",
    );

    await expect(headerFooterCard).toBeVisible();

    await expect(headerFooterCard).toHaveScreenshot(
        "card-header-footer.png",
    );

    // selectable cards
    for (let i = 0; i < 4; i++) {
        const card = page.locator(`#card-selectable-${i}`);

        await expect(card).toBeVisible();

        // default state
        await expect(card).toHaveScreenshot(
            `card-selectable-${i}-default.png`,
        );

        // selected state
        await card.click();

        await expect(card).toHaveScreenshot(
            `card-selectable-${i}-selected.png`,
        );

        // unselect again
        await card.click();
    }
});
