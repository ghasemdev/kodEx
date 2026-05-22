plugins {
    id("ktor-service-convention")
    application
}

application {
    mainClass.set("dev.kodex.sandbox.ApplicationKt")
}

dependencies {
    implementation(projects.core)
    implementation(projects.sandboxRunner.executor)
}
