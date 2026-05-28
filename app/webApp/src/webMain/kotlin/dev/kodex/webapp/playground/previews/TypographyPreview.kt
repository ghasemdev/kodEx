package dev.kodex.webapp.playground.previews

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent
import dev.kilua.html.code
import dev.kilua.html.div
import dev.kilua.html.h2
import dev.kilua.html.p
import dev.kilua.html.span

@Composable
fun IComponent.TypographyPreview() {
    div(className = "flex flex-col gap-10") {
        // ── Font families ──────────────────────────────────────────
        section(
            title = "Font Families",
            id = "typography-font-families",
        ) {
            div(className = "flex flex-col gap-4") {
                fontRow(
                    label = "Inter Tight  —  sans-serif (UI)",
                    className = "font-sans",
                    sample = "The quick brown fox jumps over the lazy dog  •  0123456789",
                )
                fontRow(
                    label = "Vazirmatn  —  فارسی",
                    className = "font-sans",
                    sample = "دانش آموز کد می‌نویسد  •  KodEx  •  ۰۱۲۳۴۵۶۷۸۹",
                    rtl = true,
                )
                fontRow(
                    label = "JetBrains Mono  —  monospace (code)",
                    className = "font-mono",
                    sample = "fun main() { println(\"Hello, World!\") }",
                )
            }
        }

        // ── Type scale ─────────────────────────────────────────────
        section(
            title = "Type Scale",
            id = "typography-type-scale",
        ) {
            div(className = FLEX_FLEX_COL_GAP_3) {
                listOf(
                    Triple("text-4xl font-bold", "4xl / Bold", "Display heading"),
                    Triple("text-3xl font-bold", "3xl / Bold", "Page title"),
                    Triple("text-2xl font-semibold", "2xl / Semibold", "Section heading"),
                    Triple("text-xl font-semibold", "xl / Semibold", "Card title"),
                    Triple("text-lg font-medium", "lg / Medium", "Sub-section"),
                    Triple("text-base font-normal", "base / Regular", "Body text"),
                    Triple("text-sm font-normal", "sm / Regular", "Secondary text"),
                    Triple("text-xs font-normal", "xs / Regular", "Caption / label"),
                ).forEach { (cls, size, role) ->
                    div(className = "flex items-baseline gap-4") {
                        span(className = "$cls text-on-surface flex-1") { +role }
                        span(className = "text-xs text-on-surface/40 w-36 shrink-0 font-mono") { +size }
                    }
                }
            }
        }

        // ── Weights ────────────────────────────────────────────────
        section(
            title = "Font Weights",
            id = "typography-font-weights",
        ) {
            div(className = "flex flex-wrap gap-6") {
                listOf(
                    "font-light" to "Light 300",
                    "font-normal" to "Regular 400",
                    "font-medium" to "Medium 500",
                    "font-semibold" to "Semibold 600",
                    "font-bold" to "Bold 700",
                    "font-extrabold" to "Extra Bold 800",
                ).forEach { (cls, label) ->
                    div(className = "flex flex-col gap-1") {
                        span(className = "text-xl $cls text-on-surface") { +"Ag" }
                        span(className = "text-xs text-on-surface/50") { +label }
                    }
                }
            }
        }

        // ── Body paragraph ─────────────────────────────────────────
        section(
            title = "Body Text",
            id = "typography-body-text",
        ) {
            div(className = "flex flex-col gap-4 max-w-2xl") {
                p(className = "text-base text-on-surface leading-relaxed") {
                    +BODY_PARAGRAPH_1
                }
                p(className = "text-sm text-on-surface/70 leading-relaxed") {
                    +BODY_PARAGRAPH_2
                }
                p(className = "text-xs text-on-surface/50") {
                    +"Caption / label text — timestamps, metadata, fine print."
                }
            }
        }

        // ── Code text ──────────────────────────────────────────────
        section(
            title = "Monospace / Code",
            id = "typography-code",
        ) {
            div(className = FLEX_FLEX_COL_GAP_3) {
                div(
                    className = "bg-surface-container rounded-lg px-4 py-3 font-mono " +
                        "text-sm text-on-surface border border-outline/20",
                ) {
                    code { +CODE_TEXT_1 }
                }
                div(
                    className = "bg-surface-container rounded-lg px-4 py-3 font-mono " +
                        "text-xs text-on-surface/70 border border-outline/20",
                ) {
                    code { +CODE_TEXT_2 }
                }
            }
        }

        // ── Color roles ────────────────────────────────────────────
        section(
            title = "Text Color Roles",
            id = "typography-color-roles",
        ) {
            div(className = "flex flex-col gap-2") {
                listOf(
                    "text-on-surface" to "on-surface  — primary content",
                    "text-on-surface/70" to "on-surface/70  — secondary",
                    "text-on-surface/50" to "on-surface/50  — tertiary / muted",
                    "text-on-surface/30" to "on-surface/30  — disabled / placeholder",
                    "text-primary" to "primary  — interactive / brand",
                    "text-error" to "error  — destructive / invalid",
                    "text-success" to "success  — confirmation / positive",
                    "text-warning" to "warning  — caution",
                    "text-info" to "info  — informational",
                ).forEach { (cls, desc) ->
                    div(className = "flex items-center gap-3") {
                        span(className = "text-base font-medium $cls w-56") { +"Aa Bb 123" }
                        span(className = "text-xs font-mono text-on-surface/40") { +desc }
                    }
                }
            }
        }
    }
}

@Composable
private fun IComponent.section(title: String, id: String, content: @Composable IComponent.() -> Unit) {
    div(
        id = id,
        className = FLEX_FLEX_COL_GAP_3,
    ) {
        div(className = "flex items-center gap-3") {
            h2(className = "text-xs font-semibold uppercase tracking-widest text-on-surface/40") { +title }
            div(className = "flex-1 h-px bg-outline/20") {}
        }
        content()
    }
}

@Composable
private fun IComponent.fontRow(label: String, className: String, sample: String, rtl: Boolean = false) {
    div(className = "flex flex-col gap-1") {
        span(className = "text-xs font-mono text-on-surface/40") { +label }
        div(
            className = "text-lg $className text-on-surface py-1",
        ) {
            if (rtl) attribute("dir", "rtl")
            +sample
        }
    }
}

private const val FLEX_FLEX_COL_GAP_3 = "flex flex-col gap-3"
private const val BODY_PARAGRAPH_1 = "KodEx is an interactive Kotlin exam platform designed for " +
    "Android developers.  Candidates write real Kotlin code in a sandboxed editor, which is then evaluated " +
    "by a configurable test-injection engine running on the server."
private const val BODY_PARAGRAPH_2 = "Secondary paragraph — smaller, muted. This style is used for descriptions, " +
    "help text, and supplementary content that supports the primary reading flow."
private const val CODE_TEXT_1 =
    "fun isDev(): Boolean = js(\"process.env.NODE_ENV === 'development'\").unsafeCast<Boolean>()"
private const val CODE_TEXT_2 = "@Composable fun IComponent.Button(variant: ButtonVariant = ButtonVariant.Primary, ...)"
