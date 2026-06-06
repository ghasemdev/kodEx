@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod")

package dev.kodex.webapp.pages.landing.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h2
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.shared.session.SessionState
import dev.kodex.shared.session.UserRole
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.ScrollTriggerConfig
import dev.kodex.webapp.gsap.gsap
import dev.kodex.webapp.gsap.prefersReducedMotion
import js.objects.unsafeJso
import kotlinx.browser.document
import kotlinx.browser.window

private const val CHECKLIST_ANIM_DURATION = 0.45
private const val CHECKLIST_STAGGER = 0.15
private const val CHECKLIST_SLIDE_X = -12.0
private const val CTA_SCROLL_START = "top 85%"

private val CHECKLIST_ITEMS = listOf(
    "Three exam types: Multiple Choice, I/O, Injection",
    "Automated grading against hidden test cases",
    "Real-time leaderboard per contest",
    "Badge and trophy rewards for top finishers",
    "Export results to CSV for your records",
)

private val CREATOR_ROLES = setOf(UserRole.EXAM_CREATOR, UserRole.ADMIN)

@Composable
fun IComponent.CreateExamSection(session: SessionState = SessionState.Guest) {
    LaunchedEffect(Unit) { animateChecklist() }

    div(
        id = "create-exam-section",
        className = "py-24 bg-neutral-950 text-white",
    ) {
        div(className = "container mx-auto px-4") {
            div(className = "max-w-lg mx-auto") {
                div(className = "text-center mb-10") {
                    span(
                        className = "text-xs font-semibold uppercase tracking-widest text-primary mb-3 block",
                    ) { +i18n.tr("For Educators & Organisers") }
                    h2(className = "text-3xl font-bold mb-4") {
                        +i18n.tr("Run Your Own Kotlin Exam")
                    }
                    p(className = "text-white/60") {
                        +i18n.tr("Create a fully automated exam in minutes and let KodEx do the grading.")
                    }
                }

                div(className = "mb-10 flex flex-col gap-3") {
                    CHECKLIST_ITEMS.forEachIndexed { idx, item ->
                        checklistItem(item, idx)
                    }
                }
            }

            div(className = "flex flex-col sm:flex-row gap-3 justify-center") {
                val canCreate = session is SessionState.Authenticated &&
                    session.roles.any { it in CREATOR_ROLES }
                val primaryLabel = if (canCreate) {
                    i18n.tr("Create an Exam →")
                } else {
                    i18n.tr("Sign Up to Create →")
                }
                val primaryPath = if (canCreate) "/exam/new" else "/signup"
                Button(
                    label = primaryLabel,
                    variant = ButtonVariant.Primary,
                    className = "w-full sm:w-auto",
                    onClick = {
                        window.history.pushState(null, "", primaryPath)
                        window.dispatchEvent(org.w3c.dom.events.Event("popstate"))
                    },
                )
                Button(
                    label = i18n.tr("See Demo"),
                    variant = ButtonVariant.Secondary,
                    className = "w-full sm:w-auto",
                    onClick = {
                        window.history.pushState(null, "", "/problems")
                        window.dispatchEvent(org.w3c.dom.events.Event("popstate"))
                    },
                )
            }
        }
    }
}

@Composable
private fun IComponent.checklistItem(text: String, idx: Int) {
    div(
        id = "checklist-item-$idx",
        className = "flex items-start gap-3",
    ) {
        span(
            className = "mt-0.5 w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 " +
                "bg-primary/20 text-primary text-[10px] font-bold",
        ) { +"✓" }
        span(className = "text-sm text-white/75 leading-relaxed") {
            +i18n.tr(text)
        }
    }
}

private fun animateChecklist() {
    if (prefersReducedMotion()) return

    val section = document.getElementById("create-exam-section") ?: return
    val isRtl = document.documentElement?.getAttribute("dir") == "rtl"
    val slideX = if (isRtl) -CHECKLIST_SLIDE_X else CHECKLIST_SLIDE_X

    repeat(CHECKLIST_ITEMS.size) { idx ->
        val item = document.getElementById("checklist-item-$idx") ?: return@repeat
        gsap.from(
            item,
            unsafeJso {
                opacity = 0.0
                x = slideX
                duration = CHECKLIST_ANIM_DURATION
                delay = idx * CHECKLIST_STAGGER
                ease = "power2.out"
                scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                    trigger = section
                    start = CTA_SCROLL_START
                    once = true
                }
            },
        )
    }
}
