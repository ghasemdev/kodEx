plugins {
    id("ktor-service-convention")
}

dependencies {
    implementation(projects.server.domain)
    implementation(projects.core.models)
    implementation(projects.core)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.kotest)
}
