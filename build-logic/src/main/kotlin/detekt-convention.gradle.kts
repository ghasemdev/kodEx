import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
//    id("io.gitlab.arturbosch.detekt")
    id("dev.detekt")
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
        markdown.required.set(true)
        sarif.required.set(true)
    }
}

dependencies {
//    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.findVersion("detekt").get()}")
    "detektPlugins"("dev.detekt:detekt-rules-ktlint-wrapper:${libs.findVersion("detekt").get()}")
}
