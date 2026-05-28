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

                    // Fix 1: vite-plugin-commonjs incorrectly treats
                    // WASM callback `require` as CommonJS require.
                    patched = patched.replace(
                        "viteCommonjs()",
                        "viteCommonjs({ filter: (id) => id.includes('import-object') ? false : undefined })",
                    )

                    // Fix 2: node_modules is symlinked outside Vite root.
                    // Without strict:false Vite blocks serving fonts/assets.
                    patched = patched.replace(
                        "host: 'localhost',",
                        "fs: { strict: false },\n\t\thost: 'localhost',",
                    )

                    if (patched != original) {
                        config.writeText(patched)
                        println("Patched vite config: ${config.name}")
                    }
                }
        }
    }
}
