plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt) apply false

    id("detekt-convention") apply false

    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kilua) apply false
}

kover {
    reports {
        total {
            verify {
                rule {
                    minBound(90)
                }
            }
        }
    }
}

tasks.register("detektAll") {
    group = "verification"
    description = "Run Detekt on all subprojects"
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("detekt") })
}
