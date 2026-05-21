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
// Source sets created by the "benchmark" compilations below:
//   src/commonBenchmark/kotlin  ← shared benchmark utilities / base classes
//   src/jvmBenchmark/kotlin     ← JVM-specific benchmarks
//   src/jsBenchmark/kotlin      ← JS-specific benchmarks
//
// Task naming (kotlinx.benchmark appends "Benchmark" to the registered name):
//   register("jvmBenchmark") → task: jvmBenchmarkBenchmark
//   register("jsBenchmark")  → task: jsBenchmarkBenchmark
//   ./gradlew benchmark      → aggregation task that runs all of the above
benchmark {
    configurations {
        named("main") {
            iterationTime = 5
            iterationTimeUnit = "sec"
        }
        // "fast" config: quick smoke-check for CI — one short iteration
        create("fast") {
            iterations = 1
            iterationTime = 500
            iterationTimeUnit = "ms"
            advanced("jvmForks", 1)
        }
    }
    targets {
        register("jvmBenchmark")
        register("jsBenchmark")
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
        compilations.create("benchmark") {
            associateWith(compilations.getByName("main"))
        }
    }

    js(IR) {
        browser {
            binaries.executable()
        }
        compilations.create("benchmark") {
            associateWith(compilations.getByName("main"))
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

        // commonBenchmark sits above all platform benchmark source sets.
        // It depends on commonMain so benchmark code can use production types.
        val commonBenchmark by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.findLibrary("kotlinx-benchmark-runtime").get())
            }
        }

        // Platform-specific benchmark source sets, each depending on commonBenchmark.
        // Kotlin creates these automatically when compilations.create("benchmark") runs
        // for the respective target — we just wire the hierarchy here.
        getByName("jvmBenchmark") {
            dependsOn(commonBenchmark)
        }

        getByName("jsBenchmark") {
            dependsOn(commonBenchmark)
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
