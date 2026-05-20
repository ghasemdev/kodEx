import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    source.setFrom(
        "src/main/kotlin",
        "src/dev/kotlin",
    )
}

configure<Detekt> {
    reports {
        html.required.set(true)
        md.required.set(true)
        sarif.required.set(true)
        txt.required.set(true)
        xml.required.set(true)
    }
}
