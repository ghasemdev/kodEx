import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

// Read machine-local Chrome path from local.properties (gitignored).
// Falls back to CHROME_BIN env var if the key is not set.
private val chromeBinEnvKey = "CHROME_BIN"
val chromeBin = Config.get("chromeBin")
    .env(chromeBinEnvKey)
    .property("chrome.bin")
    .resolve(project)

plugins {
    id("kotlin-kmp-convention")
    id("kotlin-wasm-vite-convention")

    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kilua)
    alias(libs.plugins.gettext)
    alias(libs.plugins.vite)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    js {
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
                if (chromeBin != null) environment(chromeBinEnvKey, chromeBin)
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
                if (chromeBin != null) environment(chromeBinEnvKey, chromeBin)
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
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
        }
        webMain.dependencies {
            implementation(libs.kilua)
            implementation(libs.kilua.tailwindcss)
            implementation(libs.kilua.i18n)
            implementation(libs.kilua.fontawesome)
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)

            implementation(npm("@zxcvbn-ts/core", libs.versions.zxcvbn.ts.get()))
            implementation(npm("@fontsource-variable/inter-tight", libs.versions.fontsource.get()))
            implementation(npm("@fontsource-variable/vazirmatn", libs.versions.fontsource.get()))
            implementation(npm("@fontsource-variable/jetbrains-mono", libs.versions.fontsource.get()))
            implementation(npm("highlight.js", libs.versions.highlightjs.get()))
            implementation(npm("gsap", libs.versions.gsap.get()))
            implementation(libs.kotlinx.datetime)
            implementation(npm("@js-joda/timezone", libs.versions.joda.get()))
        }
        webTest.dependencies {
            implementation(npm("html2canvas", libs.versions.html2canvas.get()))

            implementation(kotlin("test")) // Karma / browser-test-runner bridge
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
    }
}

composeCompiler {
    targetKotlinPlatforms.set(
        KotlinPlatformType.entries
            .filterNot { it == KotlinPlatformType.jvm }
            .asIterable(),
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
        proxy("/api", "http://localhost:8080")
    }
}

project.plugins.withType<NodeJsPlugin> {
    project.the<NodeJsEnvSpec>().version = libs.versions.nodejs.get()
}

project.plugins.withType<WasmNodeJsPlugin> {
    project.the<WasmNodeJsEnvSpec>().version = libs.versions.nodejs.get()
}
