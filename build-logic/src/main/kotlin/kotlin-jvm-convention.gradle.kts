import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")

    id("benchmark-convention")
    id("detekt-convention")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("kotlin.io.encoding.ExperimentalEncodingApi")
    }
    // Separate benchmark compilation — src/benchmark/kotlin — can see main classes
    // but is excluded from the production JAR. kotlinx.benchmark targets this compilation.
    val mainCompilation = target.compilations.getByName("main")
    target.compilations.create("jvm") {
        associateWith(mainCompilation)
        defaultSourceSet.kotlin.setSrcDirs(listOf("src/benchmark/kotlin"))
        defaultSourceSet.dependencies {
            implementation(libs.findLibrary("kotlinx-benchmark-runtime").get())
        }
    }
}

dependencies {
    api(libs.findLibrary("kotlinx-coroutines-core").get())
    api(libs.findLibrary("kotlinx-serialization-json").get())
}

// Benchmark target for JVM modules — configurations (main/fast) live in benchmark-convention.
// Task naming: register("jvm") → jvmBenchmark (main), jvmFastBenchmark (fast)
benchmark {
    targets {
        register("jvm")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Skip benchmark tasks for modules with no @Benchmark classes in src/benchmark/kotlin.
@Suppress("UnstableApiUsage")
afterEvaluate {
    val hasSources: Spec<Task> = Spec { _ ->
        val dir = file("src/benchmark/kotlin")
        dir.exists() && dir.walkTopDown().any { it.isFile && it.extension == "kt" }
    }
    tasks.findByName("jvmBenchmark")?.onlyIf("has benchmark sources", hasSources)
    tasks.findByName("jvmFastBenchmark")?.onlyIf("has benchmark sources", hasSources)
}
