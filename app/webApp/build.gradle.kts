import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    id("kotlin-kmp-convention")
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kilua)
    alias(libs.plugins.gettext)
    alias(libs.plugins.vite)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    js(IR) {
        useEsModules()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
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
    wasmJs {
        useEsModules()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
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

            implementation(npm("@fontsource-variable/inter-tight", libs.versions.fontsource.get()))
            implementation(npm("@fontsource-variable/vazirmatn", libs.versions.fontsource.get()))
            implementation(npm("@fontsource-variable/jetbrains-mono", libs.versions.fontsource.get()))
            implementation(npm("highlight.js", libs.versions.highlightjs.get()))
        }
    }
}

composeCompiler {
    targetKotlinPlatforms.set(
        KotlinPlatformType.entries
            .filterNot { it == KotlinPlatformType.jvm }
            .asIterable()
    )
}

gettext {
    potFile.set(File(projectDir, "src/webMain/resources/modules/i18n/messages.pot"))
    keywords.set(listOf("tr", "trn:1,2", "trc:2", "trnc:2,3", "marktr"))
}

vite {
    autoRewriteIndex.set(true)

    plugin("@tailwindcss/vite", "tailwindcss", libs.versions.tailwindcss.get())

    build {
        target = "es2020"
    }
    server {
        port = 3000
    }
}

project.plugins.withType<NodeJsPlugin> {
    project.the<NodeJsEnvSpec>().version = libs.versions.nodejs.get()
}

project.plugins.withType<WasmNodeJsPlugin> {
    project.the<WasmNodeJsEnvSpec>().version = libs.versions.nodejs.get()
}
