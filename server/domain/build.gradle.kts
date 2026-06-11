plugins {
    id("kotlin-jvm-convention")
}

dependencies {
    implementation(projects.core.models)
    implementation(libs.kotlinx.datetime)
}
