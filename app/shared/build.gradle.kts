plugins {
    id("kotlin-kmp-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.models)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime.annotation)
        }
        jsMain.dependencies {}
        wasmJsMain.dependencies {}
    }
}
