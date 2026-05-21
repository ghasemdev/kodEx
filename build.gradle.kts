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
