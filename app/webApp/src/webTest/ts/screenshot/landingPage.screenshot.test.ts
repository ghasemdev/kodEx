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

async function gotoLanding(page: import("@playwright/test").Page) {
    await page.route("**/api/v1/stats/landing", (r) =>
        r.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(statsApiMock),
        }),
    );
    await page.addStyleTag({content: noAnimationCss});
    await page.goto("http://localhost:3000/");
}

async function gotoLandingLight(page: import("@playwright/test").Page) {
    await page.addInitScript(() => {
        localStorage.setItem("kodex-theme", "light");
    });
    await gotoLanding(page);
}

test.describe("landing page screenshots", () => {
    // ─── Hero ────────────────────────────────────────────────────────────────

    test("hero left column desktop 1280px dark theme", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const leftCol = page.locator("#hero-section .flex-1").first();
        await expect(leftCol).toBeVisible();
        await expect(leftCol).toHaveScreenshot(
            "landing-hero-left-col-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("hero left column mobile 375px light theme", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const leftCol = page.locator("#hero-section .flex-1").first();
        await expect(leftCol).toBeVisible();
        await expect(leftCol).toHaveScreenshot(
            "landing-hero-left-col-mobile-light.png",
            {animations: "disabled"},
        );
    });

    // ─── GlobalNavBar ────────────────────────────────────────────────────────

    test("global navbar guest state desktop dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const navbar = page.locator("nav").first();
        await expect(navbar).toBeVisible();
        await expect(navbar).toHaveScreenshot(
            "global-navbar-guest-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("global navbar guest state mobile dark", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLanding(page);
        const navbar = page.locator("nav").first();
        await expect(navbar).toBeVisible();
        await expect(navbar).toHaveScreenshot(
            "global-navbar-guest-mobile-dark.png",
            {animations: "disabled"},
        );
    });

    // ─── ExamTypesSection ────────────────────────────────────────────────────

    test("exam types section desktop 1280px dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const section = page.locator("#exam-types-section");
        await expect(section).toBeVisible();
        await expect(section).toHaveScreenshot(
            "landing-exam-types-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("exam types section mobile 375px light", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const section = page.locator("#exam-types-section");
        await expect(section).toBeVisible();
        await expect(section).toHaveScreenshot(
            "landing-exam-types-mobile-light.png",
            {animations: "disabled"},
        );
    });

    // ─── GamificationSection ─────────────────────────────────────────────────

    test("gamification section desktop 1280px dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const section = page.locator("#gamification-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-gamification-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("gamification section mobile 375px light", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const section = page.locator("#gamification-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-gamification-mobile-light.png",
            {animations: "disabled"},
        );
    });

    // ─── LeaderboardSection ──────────────────────────────────────────────────

    test("leaderboard section desktop 1280px dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const section = page.locator("#leaderboard-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-leaderboard-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("leaderboard section mobile 375px light", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const section = page.locator("#leaderboard-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-leaderboard-mobile-light.png",
            {animations: "disabled"},
        );
    });

    // ─── ProblemsSection ─────────────────────────────────────────────────────

    test("problems section desktop 1280px dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const section = page.locator("#problems-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-problems-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("problems section mobile 375px light", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const section = page.locator("#problems-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-problems-mobile-light.png",
            {animations: "disabled"},
        );
    });

    // ─── CreateExamSection ───────────────────────────────────────────────────

    test("create exam section desktop 1280px dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const section = page.locator("#create-exam-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-create-exam-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("create exam section mobile 375px light", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const section = page.locator("#create-exam-section");
        await expect(section).toBeVisible();
        await section.scrollIntoViewIfNeeded();
        await expect(section).toHaveScreenshot(
            "landing-create-exam-mobile-light.png",
            {animations: "disabled"},
        );
    });

    // ─── Footer ──────────────────────────────────────────────────────────────

    test("footer desktop 1280px dark", async ({page}) => {
        await page.setViewportSize({width: 1280, height: 900});
        await gotoLanding(page);
        const footer = page.locator("#site-footer");
        await expect(footer).toBeVisible();
        await footer.scrollIntoViewIfNeeded();
        await expect(footer).toHaveScreenshot(
            "landing-footer-desktop-dark.png",
            {animations: "disabled"},
        );
    });

    test("footer mobile 375px light", async ({page}) => {
        await page.setViewportSize({width: 375, height: 812});
        await gotoLandingLight(page);
        const footer = page.locator("#site-footer");
        await expect(footer).toBeVisible();
        await footer.scrollIntoViewIfNeeded();
        await expect(footer).toHaveScreenshot(
            "landing-footer-mobile-light.png",
            {animations: "disabled"},
        );
    });
});
