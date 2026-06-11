plugins {
    id("ktor-service-convention")
}

dependencies {
    implementation(projects.server.domain)
    api(projects.core.models)
    implementation(projects.core)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.forwarded.header)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.rate.limit)
    testImplementation(libs.bundles.kotest)
}
