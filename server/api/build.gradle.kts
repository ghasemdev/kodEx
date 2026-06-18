plugins {
    id("ktor-service-convention")
    alias(libs.plugins.koin.compiler)
}

dependencies {
    implementation(projects.server.domain)
    api(projects.core.models)
    implementation(projects.core)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.koin.annotations)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.kotest)
}
