plugins {
    id("kotlin-kmp-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.models)
        }
    }
}
