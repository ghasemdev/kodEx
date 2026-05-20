plugins {
    `ktor-service-convention`
}

application {
    mainClass.set("dev.kodex.sandbox.ApplicationKt")
}

dependencies {
    implementation(projects.sandboxRunner.executor)
}
