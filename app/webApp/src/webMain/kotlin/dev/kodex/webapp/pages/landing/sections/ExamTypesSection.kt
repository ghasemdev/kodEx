@file:Suppress("MagicNumber", "LabeledExpression")

package dev.kodex.webapp.pages.landing.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h2
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.core.models.ExamType
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.ScrollTriggerConfig
import dev.kodex.webapp.gsap.gsap
import dev.kodex.webapp.gsap.prefersReducedMotion
import js.objects.unsafeJso
import kotlinx.browser.document

private const val CARD_ANIM_DURATION = 0.5
private const val CARD_STAGGER_DELAY = 0.12

private data class ExamCardSpec(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val snippet: String,
)

private val EXAM_CARDS = listOf(
    ExamCardSpec(
        id = "exam-type-card-0",
        titleKey = ExamType.QUIZ.displayName,
        descKey = "Answer multiple-choice questions within a strict time limit. " +
            "Each question delivers instant feedback and a detailed explanation, " +
            "so you reinforce the concept immediately. Ideal for testing theoretical " +
            "knowledge, language syntax, and SDK fundamentals.",
        snippet = "question.answer == correct",
    ),
    ExamCardSpec(
        id = "exam-type-card-1",
        titleKey = ExamType.IO.displayName,
        descKey = "Write algorithms that read from standard input and print the correct output. " +
            "Solutions are validated against hidden test cases covering normal inputs, " +
            "boundary conditions, and adversarial edge cases — " +
            "exactly the format used in competitive programming contests.",
        snippet = "readLine() → solve() → println(result)",
    ),
    ExamCardSpec(
        id = "exam-type-card-2",
        titleKey = ExamType.INJECTION.displayName,
        descKey = "Implement functions that are injected directly into a hidden test harness. " +
            "Your code must satisfy a full automated test suite written by the exam creator — " +
            "the same paradigm used in professional TDD workflows and code-review pipelines.",
        snippet = "fun solve(n: Int): Int",
    ),
)

@Composable
fun IComponent.ExamTypesSection() {
    LaunchedEffect(Unit) { animateExamCards() }

    div(className = "py-24 bg-surface-container/30") {
        div(className = "container mx-auto px-4") {
            div(className = "text-center mb-12") {
                h2(className = "text-3xl font-bold text-on-surface mb-3") {
                    +i18n.tr("Three ways to challenge developers")
                }
                p(className = "text-on-surface/60 max-w-xl mx-auto") {
                    +i18n.tr("Each exam type tests a different dimension of engineering skill.")
                }
            }
            div(className = "grid grid-cols-1 md:grid-cols-3 gap-6") {
                EXAM_CARDS.forEach { card -> examCard(card) }
            }
        }
    }
}

@Composable
private fun IComponent.examCard(spec: ExamCardSpec) {
    div(
        id = spec.id,
        className = "flex flex-col gap-4 p-6 rounded-2xl bg-surface-container " +
            "border border-outline/20 hover:border-primary/40 transition-colors duration-200 " +
            "hover:shadow-lg hover:shadow-primary/5",
    ) {
        span(className = "text-lg font-semibold text-on-surface") {
            +i18n.tr(spec.titleKey)
        }
        p(className = "text-sm text-on-surface/60 leading-relaxed") {
            +i18n.tr(spec.descKey)
        }
        div(
            className = "font-mono text-xs px-3 py-2 rounded-lg bg-neutral-900 text-success " +
                "border border-outline/10 mt-auto",
        ) {
            +spec.snippet
        }
    }
}

private fun animateExamCards() {
    if (prefersReducedMotion()) return
    EXAM_CARDS.forEachIndexed { idx, spec ->
        val element = document.getElementById(spec.id) ?: return@forEachIndexed
        gsap.from(
            element,
            unsafeJso {
                y = 40.0
                opacity = 0.0
                duration = CARD_ANIM_DURATION
                delay = idx * CARD_STAGGER_DELAY
                ease = "back.out(1.2)"
                scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                    trigger = element
                    start = "top 82%"
                    once = true
                }
            },
        )
    }
}
