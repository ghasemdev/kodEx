plugins {
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.kotlin.benchmark) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dependencycheck)

    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kilua) apply false

    id("detekt-convention") apply false
}

dependencyCheck {
    nvd {
        val apiKey = Config.get("nvdApiKey")
            .env("NVD_API_KEY")
            .property("nvd.apiKey")
            .resolve(project)

        if (apiKey.isNullOrBlank()) {
            logger.warn("⚠️ NVD API Key is missing!")
        }

        apiKey?.let {
            this.apiKey.set(it)
        }
    }
    analyzers {
        assemblyEnabled = false
    }

    failBuildOnCVSS.set(7f)

    outputDirectory.set(
        rootProject.layout.projectDirectory
            .dir("build/reports/dependency-check")
    )

    suppressionFile = rootProject.layout.projectDirectory
        .file("config/dependency-check/dependency-check-suppressions.xml")
        .asFile
        .path

    autoUpdate = false
}

dependencies {
    kover(projects.core.models)
    kover(projects.server.api)
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
            verify {
                rule {
                    minBound(90)
                }
            }
        }
    }
}
