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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kodex"

include(
    ":core",
    ":core:data",
    ":app:shared",
    ":app:webApp",
    ":server:app",
    ":server:api",
    ":server:domain",
    ":server:data",
    ":sandbox-runner:app",
    ":sandbox-runner:executor",
)
