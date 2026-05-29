plugins {
    id("ktor-service-convention")
    application

    alias(libs.plugins.koin.compiler)
}

application {
    mainClass.set("dev.kodex.server.ApplicationKt")
}

sourceSets {
    create("devMain") {
        kotlin.srcDir("src/dev/kotlin")
        resources.srcDir("src/dev/resources")
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
    }
}

tasks.register<JavaExec>("devRun") {
    classpath = sourceSets["devMain"].runtimeClasspath
    mainClass.set("dev.kodex.server.ApplicationKt")
    systemProperty("logback.configurationFile", "logback-dev.xml")
}

tasks.named<Sync>("installDist") {
    // production dist uses only main source set — devMain is excluded automatically
    // since devMain is not part of the application runtime classpath
}

dependencies {
    implementation(projects.server.api)
    implementation(projects.server.domain)
    implementation(projects.server.data)
    implementation(projects.core)

    "devMainImplementation"(sourceSets.main.get().output)
    "devMainImplementation"(sourceSets.main.get().runtimeClasspath)
}
