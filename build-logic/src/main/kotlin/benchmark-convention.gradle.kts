plugins {
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlinx.benchmark")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

// Shared benchmark configurations for all JVM/KMP modules.
// Targets are registered by each consuming convention (kmp or jvm).
// fast: CI-optimized — 1 iteration / 500 ms / 1 fork skips full JMH warmup overhead.
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
}
