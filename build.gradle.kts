plugins {
    // version-pins for plugins applied directly in module build.gradle.kts files
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kilua) apply false
    alias(libs.plugins.vite) apply false

    id("kover-report-convention")
    id("dependency-check-convention")
    id("benchmark-aggregation-convention")
}

dependencies {
    kover(projects.core.models)
    kover(projects.server.api)
}
