tasks.register<Exec>("screenshotsVitest") {
    group = "verification"
    description = "Run all Playwright screenshot tests."

    workingDir("${rootProject.projectDir}/app/webApp")

    commandLine(
        Config.get("npxPath")
            .env("NPX_PATH")
            .property("npx.path")
            .resolve(project)
            ?: "npx",
        "playwright",
        "test",
    )
}
