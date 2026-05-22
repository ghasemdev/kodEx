plugins {
    id("org.owasp.dependencycheck")
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
