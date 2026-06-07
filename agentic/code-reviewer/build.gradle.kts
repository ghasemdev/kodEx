plugins {
    id("kotlin-jvm-convention")
    application
}

application {
    mainClass.set("dev.kodex.agentic.code.reviewer.MainKt")
}

kotlin {
    dependencies {
        implementation("ai.koog:koog-agents:1.0.0")
        implementation("ai.koog:koog-agents-additions:1.0.0-beta")
    }
}
