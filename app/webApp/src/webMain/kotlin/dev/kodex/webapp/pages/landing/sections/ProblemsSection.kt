@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod")

package dev.kodex.webapp.pages.landing.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h2
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.core.models.DifficultyTier
import dev.kodex.core.models.ExamType
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.ScrollTriggerConfig
import dev.kodex.webapp.gsap.gsap
import dev.kodex.webapp.pages.landing.model.PlaceholderProblems
import dev.kodex.webapp.pages.landing.model.ProblemSummary
import js.objects.unsafeJso
import kotlinx.browser.document

private const val ROW_ANIM_DURATION = 0.45
private const val ROW_STAGGER = 0.06
private const val ROW_SLIDE_X = -20.0
private const val PROBLEMS_SCROLL_START = "top 85%"

private val FILTER_CHIPS = listOf("All", "Kotlin", "Android", "Easy", "Medium", "Hard")

@Composable
fun IComponent.ProblemsSection() {
    LaunchedEffect(Unit) { animateProblemRows() }

    div(id = "problems-section", className = "py-24 bg-surface") {
        div(className = "container mx-auto px-4") {
            div(className = "flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8") {
                div {
                    h2(className = "text-3xl font-bold text-on-surface mb-1") {
                        +i18n.tr("Problem Dataset")
                    }
                    p(className = "text-on-surface/60") {
                        +i18n.tr("Curated Kotlin & Android challenges at every level.")
                    }
                }
                div(
                    className = "flex px-3 py-1.5 text-sm cursor-pointer text-primary " +
                        "hover:bg-primary/10 rounded-lg transition-colors duration-150 w-fit",
                ) {
                    role("link")
                    tabindex(0)
                    +i18n.tr("Explore all problems →")
                }
            }

            div(className = "flex flex-wrap gap-2 mb-6") {
                FILTER_CHIPS.forEach { chip ->
                    span(
                        className = "px-3 py-1 rounded-full text-xs font-medium " +
                            "bg-surface-variant text-on-surface/60 border border-outline/10 " +
                            "cursor-default select-none",
                    ) { +i18n.tr(chip) }
                }
            }

            div(className = "rounded-2xl border border-outline/20 overflow-hidden") {
                problemsHeader()
                PlaceholderProblems.entries.forEachIndexed { idx, problem ->
                    problemRow(problem, idx)
                }
            }
        }
    }
}

@Composable
private fun IComponent.problemsHeader() {
    div(
        className = "grid grid-cols-[1fr_auto_auto] gap-4 px-4 py-2 " +
            "bg-surface-variant/50 text-[10px] uppercase tracking-wider text-on-surface/40",
    ) {
        span { +i18n.tr("Title") }
        span(className = "hidden sm:block") { +i18n.tr("Type") }
        span { +i18n.tr("Difficulty") }
    }
}

@Composable
private fun IComponent.problemRow(problem: ProblemSummary, idx: Int) {
    div(
        id = "problem-row-$idx",
        className = "grid grid-cols-[1fr_auto_auto] gap-4 px-4 py-3 items-center " +
            "border-t border-outline/10 hover:bg-surface-variant/30 transition-colors duration-100 " +
            "cursor-pointer",
    ) {
        span(className = "text-sm font-medium text-on-surface truncate") {
            +i18n.tr(problem.title)
        }
        span(
            className = "hidden sm:block text-[10px] font-semibold px-2 py-0.5 rounded-full " +
                examTypeBadgeClass(problem.type),
        ) { +i18n.tr(problem.type.displayName) }
        span(
            className = "text-[10px] font-semibold px-2 py-0.5 rounded-full " +
                difficultyBadgeClass(problem.difficulty),
        ) { +i18n.tr(problem.difficulty.display) }
    }
}

private fun examTypeBadgeClass(type: ExamType): String = when (type) {
    ExamType.QUIZ -> "bg-primary/10 text-primary"
    ExamType.IO -> "bg-secondary/10 text-secondary"
    ExamType.INJECTION -> "bg-success/10 text-success"
}

private fun difficultyBadgeClass(tier: DifficultyTier): String = when (tier) {
    DifficultyTier.EASY -> "bg-success/10 text-success"
    DifficultyTier.MEDIUM -> "bg-warning/10 text-warning"
    DifficultyTier.HARD -> "bg-error/10 text-error"
}

private fun animateProblemRows() {
    val section = document.getElementById("problems-section") ?: return
    repeat(PlaceholderProblems.entries.size) { idx ->
        val row = document.getElementById("problem-row-$idx") ?: return@repeat
        gsap.from(
            row,
            unsafeJso {
                opacity = 0.0
                x = ROW_SLIDE_X
                duration = ROW_ANIM_DURATION
                delay = idx * ROW_STAGGER
                ease = "power1.out"
                scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                    trigger = section
                    start = PROBLEMS_SCROLL_START
                    once = true
                }
            },
        )
    }
}
