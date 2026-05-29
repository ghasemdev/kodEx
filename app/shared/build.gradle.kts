plugins {
    id("kotlin-kmp-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.models)
            implementation(libs.kotlinx.coroutines.core)
        }
        jsMain.dependencies {}
        wasmJsMain.dependencies {}
    }
}
