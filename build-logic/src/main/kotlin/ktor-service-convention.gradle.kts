plugins {
    id("kotlin-jvm-convention")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findBundle("ktor-server").get())
    implementation(libs.findBundle("koin-server").get())
    implementation(libs.findBundle("logging").get())
}
