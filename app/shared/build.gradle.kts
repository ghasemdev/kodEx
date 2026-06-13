plugins {
    id("kotlin-kmp-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.models)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
        }
        jsMain.dependencies {}
        wasmJsMain.dependencies {}
    }
}
