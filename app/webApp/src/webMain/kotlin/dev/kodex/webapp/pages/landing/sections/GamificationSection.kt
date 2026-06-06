@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod", "CognitiveComplexMethod")

package dev.kodex.webapp.pages.landing.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h2
import dev.kilua.html.img
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.core.models.Tier
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.ScrollTriggerConfig
import dev.kodex.webapp.gsap.gsap
import dev.kodex.webapp.pages.landing.model.BadgeDefinition
import dev.kodex.webapp.pages.landing.model.Badges
import js.objects.unsafeJso
import kotlinx.browser.document

private const val TIER_FILL_PERCENT = "30%"
private const val TIER_FILL_DURATION = 1.2
private const val BADGE_ANIM_DURATION = 0.5
private const val BADGE_STAGGER = 0.08
private const val SECTION_SCROLL_START = "top 82%"
private const val STYLE = "style"

private val TIERS = listOf(
    Tier.JUNIOR to "text-gray-400",
    Tier.SENIOR to "text-green-500",
    Tier.MASTER to "text-orange-400",
    Tier.GRANDMASTER to "text-purple-500",
)

@Composable
fun IComponent.GamificationSection() {
    LaunchedEffect(Unit) { animateGamification() }

    div(id = "gamification-section", className = "py-24 bg-surface") {
        div(className = "container mx-auto px-4") {
            div(className = "text-center mb-12") {
                h2(className = "text-3xl font-bold text-on-surface mb-3") {
                    +i18n.tr("Earn Your Rank")
                }
                p(className = "text-on-surface/60 max-w-xl mx-auto") {
                    +i18n.tr("Solve problems, win contests, and climb the tier ladder.")
                }
            }

            tierBar()

            div(className = "mt-16 mb-4") {
                span(className = "text-xs font-semibold uppercase tracking-widest text-on-surface/40") {
                    +i18n.tr("Achievement Badges")
                }
            }
            div(className = "grid grid-cols-2 sm:grid-cols-4 gap-4") {
                Badges.all.forEachIndexed { _, badge -> badgeCard(badge) }
            }

        }
    }
}

@Composable
private fun IComponent.tierBar() {
    div(className = "rounded-2xl border border-outline/20 bg-surface-container p-6") {
        div(className = "flex justify-between mb-3") {
            TIERS.forEach { (tier, colorClass) ->
                span(className = "text-xs font-semibold $colorClass") {
                    +i18n.tr(tier.displayName)
                }
            }
        }
        div(className = "relative h-3 rounded-full overflow-hidden") {
            // Gradient spans the full track width so tier colors stay aligned with labels above
            div(className = "absolute inset-0 rounded-full") {
                attribute(
                    STYLE,
                    "background: linear-gradient(to right," +
                        " #9ca3af 0%, #22c55e 33%, #fb923c 67%, #a855f7 100%);",
                )
            }
            // Mask covers from the right; GSAP shrinks it to reveal the gradient
            div(
                id = "gamification-tier-fill",
                className = "absolute inset-y-0 end-0 bg-surface-variant",
            ) {
                attribute(STYLE, "width: 100%;")
            }
        }
        div(className = "flex justify-between mt-2") {
            span(className = "text-[10px] text-on-surface/30") { +i18n.tr("Beginner") }
            span(className = "text-[10px] text-on-surface/30") { +i18n.tr("Elite") }
        }
    }
}

@Composable
private fun IComponent.badgeCard(badge: BadgeDefinition) {
    var flipped by remember { mutableStateOf(false) }

    div(
        id = "badge-card-${badge.id}",
        className = "badge-card group relative h-52 cursor-pointer select-none rounded-2xl",
    ) {
        tabindex(0)
        role("button")
        attribute("aria-label", "${i18n.tr(badge.name)} — ${i18n.tr("press Enter to reveal unlock condition")}")
        attribute(STYLE, "perspective: 700px;")

        // Inner flipper
        div(className = "relative w-full h-full") {
            attribute(
                STYLE,
                "transform-style: preserve-3d; transition: transform 0.4s ease; " +
                    "transform: rotateY(${if (flipped) 180 else 0}deg);",
            )

            // Front face
            div(
                className = "absolute inset-0 flex items-center justify-center overflow-hidden " +
                    "rounded-2xl border border-outline/20 bg-surface-container p-2",
            ) {
                attribute(STYLE, "backface-visibility: hidden;")
                img(
                    src = "/${badge.iconPath}",
                    alt = i18n.tr(badge.name),
                    className = "w-full h-full object-contain transition-transform duration-300 group-hover:scale-110",
                ) {}
            }

            // Back face
            div(
                className = "absolute inset-0 flex flex-col items-center justify-center gap-2 " +
                    "rounded-2xl border border-primary/30 bg-primary/10 p-4",
            ) {
                attribute(STYLE, "backface-visibility: hidden; transform: rotateY(180deg);")
                span(className = "text-xs text-primary font-semibold text-center leading-snug") {
                    +i18n.tr(badge.unlockCondition)
                }
            }
        }

        onClick { flipped = !flipped }
        onKeydown { e ->
            if (e.key == "Enter" || e.key == " ") flipped = !flipped
        }
    }
}

private fun animateGamification() {
    document.getElementById("gamification-tier-fill")?.let { fill ->
        gsap.to(
            fill,
            unsafeJso {
                width = TIER_FILL_PERCENT
                duration = TIER_FILL_DURATION
                ease = "power2.inOut"
                scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                    trigger = fill
                    start = SECTION_SCROLL_START
                    once = true
                }
            },
        )
    }

    Badges.all.forEachIndexed { idx, badge ->
        val card = document.getElementById("badge-card-${badge.id}") ?: return@forEachIndexed
        gsap.from(
            card,
            unsafeJso {
                y = 20.0
                opacity = 0.0
                duration = BADGE_ANIM_DURATION
                delay = idx * BADGE_STAGGER
                ease = "power1.out"
                scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                    trigger = card
                    start = SECTION_SCROLL_START
                    once = true
                }
            },
        )
    }
}
