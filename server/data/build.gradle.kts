plugins {
    id("kotlin-jvm-convention")
    alias(libs.plugins.koin.compiler)
}

dependencies {
    implementation(projects.server.domain)
    implementation(projects.core)
    implementation(projects.core.models)

    implementation(libs.atomicfu)
    implementation(libs.koin.annotations)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.database)
    implementation(libs.lettuce.core)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.minio.sdk)
    implementation(libs.argon2.jvm)
    implementation(libs.webauthn4j.core)
    implementation(libs.kotlin.onetimepassword.lib)
    implementation(libs.zxcvbn4j.lib)
    implementation(libs.geoip2.lib)
    implementation(libs.ua.parser.java)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)
}
