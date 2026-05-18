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

dependencies {
    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.findVersion("detekt").get()}")
}
