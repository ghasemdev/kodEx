import {defineConfig} from "@playwright/test";

export default defineConfig({
    testDir: "./src/webTest/ts/screenshot",

    use: {
        baseURL: "http://localhost:3000",
        headless: true,
    }
});
