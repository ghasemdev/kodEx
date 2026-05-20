plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt) apply false

    id("detekt-convention") apply false

    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kilua) apply false
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
