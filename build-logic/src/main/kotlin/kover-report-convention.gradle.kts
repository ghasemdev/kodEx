plugins {
    id("org.jetbrains.kotlinx.kover")
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
        filters {
            excludes {
                annotatedBy("org.openjdk.jmh.annotations.State")
            }
        }
    }
}
