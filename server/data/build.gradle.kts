plugins {
    id("kotlin-jvm-convention")
}

dependencies {
    implementation(project(":server:domain"))
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.database)
    implementation(libs.lettuce.core)
}
