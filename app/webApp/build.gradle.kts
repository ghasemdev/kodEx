import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kotlin-kmp-convention")
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kilua)
}

kotlin {
    js(IR) {
        useEsModules()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
                outputFileName = "main.bundle.js"
                sourceMaps = false
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
        compilerOptions {
            target.set("es2015")
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        useEsModules()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
                outputFileName = "main.bundle.js"
                sourceMaps = false
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
        compilerOptions {
            target.set("es2015")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.napier)
            implementation(libs.ktor.client.core)
        }
        webMain.dependencies {
            implementation(libs.kilua)
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
        }
    }
}
