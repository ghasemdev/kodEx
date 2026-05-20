plugins {
    id("kotlin-jvm-convention")
}

dependencies {
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.kotlin.dot.env)
    implementation(libs.logback.classic)
}
