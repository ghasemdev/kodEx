import {expect, test} from "@playwright/test";

const statsApiMock = {
    data: {
        totalProblems: 1247,
        totalUsers: 8432,
        totalContests: 342,
    },
    meta: {
        requestId: "test-req",
        timestamp: "2026-06-07T00:00:00Z",
        service: "kodex-api",
        serviceVersion: "0.1.0",
    },
};

const noAnimationCss = `
    *,
    *::before,
    *::after {
        transition: none !important;
        animation: none !important;
    }
`;

test.describe("landing page screenshots", () => {
    test("hero left column desktop 1280px dark theme", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await page.route("**/api/v1/stats/landing", (r) =>
            r.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(statsApiMock),
            }),
        );
        await page.addStyleTag({content: noAnimationCss});
        await page.goto("http://localhost:3000/");

        // screenshot the static left column (headline + tagline + CTAs + stat counters)
        const leftCol = page.locator("#hero-section .flex-1").first();
        await expect(leftCol).toBeVisible();
        await expect(leftCol).toHaveScreenshot(
            "landing-hero-left-col-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("hero left column mobile 375px light theme", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await page.addInitScript(() => {
            localStorage.setItem("kodex-theme", "light");
        });
        await page.route("**/api/v1/stats/landing", (r) =>
            r.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(statsApiMock),
            }),
        );
        await page.addStyleTag({content: noAnimationCss});
        await page.goto("http://localhost:3000/");

        const leftCol = page.locator("#hero-section .flex-1").first();
        await expect(leftCol).toBeVisible();
        await expect(leftCol).toHaveScreenshot(
            "landing-hero-left-col-mobile-light.png",
            {animations: "disabled"},
        );
    });

    test("global navbar guest state desktop dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await page.route("**/api/v1/stats/landing", (r) =>
            r.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(statsApiMock),
            }),
        );
        await page.addStyleTag({content: noAnimationCss});
        await page.goto("http://localhost:3000/");

        // The GlobalNavBar renders as the first <nav> in the page
        const navbar = page.locator("nav").first();
        await expect(navbar).toBeVisible();
        await expect(navbar).toHaveScreenshot(
            "global-navbar-guest-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("global navbar guest state mobile dark", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await page.route("**/api/v1/stats/landing", (r) =>
            r.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(statsApiMock),
            }),
        );
        await page.addStyleTag({content: noAnimationCss});
        await page.goto("http://localhost:3000/");

        const navbar = page.locator("nav").first();
        await expect(navbar).toBeVisible();
        await expect(navbar).toHaveScreenshot(
            "global-navbar-guest-mobile-dark.png",
            {animations: "disabled"},
        );
    });
});
