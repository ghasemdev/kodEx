plugins {
    id("kotlin-jvm-convention")
}

dependencies {
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.logback.classic)
}
