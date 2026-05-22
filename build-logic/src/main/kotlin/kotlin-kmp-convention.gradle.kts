import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")

    id("benchmark-convention")
    id("detekt-convention")
    id("org.jetbrains.kotlinx.kover")
}

// Benchmark target for KMP modules — configurations (main/fast) live in benchmark-convention.
// Access to jvmMain comes from associateWith() in the compilation block below;
// no extra dependsOn() needed (explicit cross-tree dependsOn is rejected by KMP hierarchy).
// Task naming: register("jvmBenchmark") → jvmBenchmarkBenchmark (main), jvmBenchmarkFastBenchmark (fast)
benchmark {
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

        // jvmBenchmark source set is auto-created by compilations.create("benchmark") above.
        // Runtime dep only — main/commonMain visibility is via associateWith at compilation level.
        getByName("jvmBenchmark") {
            dependencies {
                implementation(libs.findLibrary("kotlinx-benchmark-runtime").get())
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Skip benchmark tasks for modules with no @Benchmark classes in src/jvmBenchmark/kotlin.
@Suppress("UnstableApiUsage")
afterEvaluate {
    val hasSources: Spec<Task> = Spec { _ ->
        val dir = file("src/jvmBenchmark/kotlin")
        dir.exists() && dir.walkTopDown().any { it.isFile && it.extension == "kt" }
    }
    tasks.findByName("jvmBenchmarkBenchmark")?.onlyIf("has benchmark sources", hasSources)
    tasks.findByName("jvmBenchmarkFastBenchmark")?.onlyIf("has benchmark sources", hasSources)
}
