pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kodex"

include(
    ":core",
    ":app:shared",
    ":app:webApp",
    ":server:app",
    ":server:api",
    ":server:domain",
    ":server:data",
    ":sandbox-runner:app",
    ":sandbox-runner:executor",
)
