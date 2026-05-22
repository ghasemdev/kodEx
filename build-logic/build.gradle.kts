plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.kotlin.serialization)
    implementation(libs.gradle.plugin.kotlin.allopen)
    implementation(libs.gradle.plugin.kotlin.benchmark)

    implementation(libs.gradle.plugin.kilua)

    implementation(libs.gradle.plugin.detekt)
    implementation(libs.gradle.plugin.kover)
    implementation(libs.gradle.plugin.dependencycheck)
}
