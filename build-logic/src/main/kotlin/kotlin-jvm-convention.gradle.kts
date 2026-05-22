import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.allopen")

    id("org.jetbrains.kotlinx.benchmark")
    id("org.jetbrains.kotlinx.kover")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
    // Separate benchmark compilation — src/benchmark/kotlin — can see main classes but
    // is excluded from the production JAR. kotlinx.benchmark targets this compilation.
    val mainCompilation = target.compilations.getByName("main")
    target.compilations.create("jvm") {
        associateWith(mainCompilation)
        defaultSourceSet.kotlin.setSrcDirs(listOf("src/benchmark/kotlin"))
        // The runtime must be declared here so the plugin puts it on the
        // execution classpath of the jvmBenchmark task (JvmBenchmarkRunnerKt).
        defaultSourceSet.dependencies {
            implementation(libs.findLibrary("kotlinx-benchmark-runtime").get())
        }
    }
}

dependencies {
    api(libs.findLibrary("kotlinx-coroutines-core").get())
    api(libs.findLibrary("kotlinx-serialization-json").get())

    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.findVersion("detekt").get()}")
}

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
        register("jvm")
    }
}

configure<AllOpenExtension> {
    annotation("org.openjdk.jmh.annotations.State")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Skip benchmark tasks when there are no @Benchmark classes in src/benchmark/kotlin.
// Without this, JMH throws "No benchmarks to run" and the task fails.
@Suppress("UnstableApiUsage")
afterEvaluate {
    val hasSources: Spec<Task> = Spec { _ ->
        val dir = file("src/benchmark/kotlin")
        dir.exists() && dir.walkTopDown().any { it.isFile && it.extension == "kt" }
    }
    tasks.findByName("jvmBenchmark")?.onlyIf("has benchmark sources", hasSources)
    tasks.findByName("jvmFastBenchmark")?.onlyIf("has benchmark sources", hasSources)
}
