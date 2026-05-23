import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kotlin-kmp-convention")
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kilua)
    alias(libs.plugins.gettext)
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
            implementation(libs.kilua.tailwindcss)
            implementation(libs.kilua.i18n)
            implementation(libs.kilua.fontawesome)
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
        }
    }
}

gettext {
    potFile.set(File(projectDir, "src/webMain/resources/modules/i18n/messages.pot"))
    keywords.set(listOf("tr", "trn:1,2", "trc:2", "trnc:2,3", "marktr"))
}
