import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.withType<Detekt>().configureEach {
    source += files(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/jsMain/kotlin",
        "src/wasmJsMain/kotlin",
        "src/webMain/kotlin",
    ).asFileTree
    reports {
        html.required.set(true)
        md.required.set(true)
        sarif.required.set(true)
        txt.required.set(true)
        xml.required.set(true)
    }
}

dependencies {
    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.findVersion("detekt").get()}")
}
