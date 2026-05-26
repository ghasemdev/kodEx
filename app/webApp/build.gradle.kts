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
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kilua)
    alias(libs.plugins.gettext)
    alias(libs.plugins.vite)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
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
        webTest.dependencies {
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
    }
}

// vite-plugin-commonjs cannot statically analyze `require(mod)` in the Kotlin/WASM
// import-object files because `require` there is a WASM callback parameter, not a CJS
// require. Patch the generated vite.config.mjs after each configure task to add a filter
// that skips those files entirely.
val viteConfigureTasks = setOf(
    "wasmJsViteConfigureDev",
    "wasmJsViteConfigureProd",
    "jsViteConfigureDev",
    "jsViteConfigureProd",
)
tasks.configureEach {
    if (name in viteConfigureTasks) {
        doLast {
            outputs.files
                .filter { it.name.endsWith(".mjs") && it.exists() }
                .forEach { config ->
                    val original = config.readText()
                    var patched = original
                    // Fix 1: vite-plugin-commonjs incorrectly treats WASM callback `require` as CJS
                    patched = patched.replace(
                        "viteCommonjs()",
                        "viteCommonjs({ filter: (id) => id.includes('import-object') ? false : undefined })",
                    )
                    // Fix 2: node_modules is a symlink whose real path is outside Vite root.
                    // Without strict:false Vite's fs policy blocks serving webfonts (FA, fontsource).
                    patched = patched.replace(
                        "host: 'localhost',",
                        "fs: { strict: false },\n\t\thost: 'localhost',",
                    )
                    if (patched != original) config.writeText(patched)
                }
        }
    }
}

project.plugins.withType<NodeJsPlugin> {
    project.the<NodeJsEnvSpec>().version = libs.versions.nodejs.get()
}

project.plugins.withType<WasmNodeJsPlugin> {
    project.the<WasmNodeJsEnvSpec>().version = libs.versions.nodejs.get()
}
