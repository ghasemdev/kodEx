import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.allopen")

    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.kotlinx.kover")
    id("io.gitlab.arturbosch.detekt")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

// ─── Benchmark configuration ───────────────────────────────────────────────
// Source set auto-created by compilations.create("benchmark") under jvm:
//   src/jvmBenchmark/kotlin  ← JVM benchmark classes (@State, @Benchmark)
//
// Access to jvmMain (and transitively commonMain) comes from associateWith()
// at the compilation level — no extra dependsOn() needed or allowed
// (explicit cross-tree dependsOn is rejected by the Kotlin hierarchy template).
//
// JS benchmark requires nodejs() on the target; add it per-module if needed.
//
// Task naming: register("jvmBenchmark") → task jvmBenchmarkBenchmark
//              ./gradlew benchmark      → aggregation task
benchmark {
    configurations {
        named("main") {
            iterationTime = 5
            iterationTimeUnit = "sec"
        }
        create("fast") {
            iterations = 1
            iterationTime = 500
            iterationTimeUnit = "ms"
            advanced("jvmForks", 1)
        }
    }
    targets {
        register("jvmBenchmark")
    }
}

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                    optIn.add("kotlin.time.ExperimentalTime")
                }
            }
        }
        val jvmMain = compilations.getByName("main")
        compilations.create("benchmark") {
            associateWith(jvmMain)
        }
    }

    js(IR) {
        browser {
            binaries.executable()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.findLibrary("kotlinx-serialization-json").get())
            api(libs.findLibrary("kotlinx-coroutines-core").get())
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // jvmBenchmark is auto-created by compilations.create("benchmark") above.
        // Only the runtime dependency is needed here; main/commonMain visibility
        // is handled by associateWith at the compilation level.
        getByName("jvmBenchmark") {
            dependencies {
                implementation(libs.findLibrary("kotlinx-benchmark-runtime").get())
            }
        }
    }
}

dependencies {
    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.findVersion("detekt").get()}")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}
