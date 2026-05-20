plugins {
    `kotlin-kmp-convention`
    alias(libs.plugins.kilua)
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }
    }
    wasmJs {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.kilua)
            implementation(libs.napier)
            implementation(libs.kotlinx.browser)
            implementation(libs.ktor.client.core)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
