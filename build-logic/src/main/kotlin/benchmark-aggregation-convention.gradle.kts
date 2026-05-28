val benchmarkConfigName: String = providers.gradleProperty("benchmarkConfig").getOrElse("main")

tasks.register("benchmarkMerge") {
    group = "benchmark"
    description = "Merge all benchmark JSONs from all modules (use -PbenchmarkConfig=fast for CI)"

    doLast {
        val configPath = benchmarkConfigName
        val jsonFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.path.contains("build/reports/benchmarks/$configPath") }
            .toList()

        val outputFile = file("build/reports/benchmarks/benchmark-results.json")
        outputFile.parentFile.mkdirs()

        outputFile.writeText(
            jsonFiles.joinToString(prefix = "[", postfix = "]", separator = ",") { f ->
                f.readText().trim().removeSurrounding("[", "]")
            },
        )

        println("Merged ${jsonFiles.size} benchmark file(s) [config: $configPath]")
        jsonFiles.forEach { println("  ${it.path}") }
    }
}
