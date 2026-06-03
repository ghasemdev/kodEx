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

private const val TIER_FILL_PERCENT = "50%"
private const val TIER_FILL_DURATION = 1.2
private const val BADGE_ANIM_DURATION = 0.5
private const val BADGE_STAGGER = 0.08
private const val TROPHY_ANIM_DURATION = 0.4
private const val TROPHY_STAGGER = 0.1
private const val SECTION_SCROLL_START = "top 82%"

private val TIERS = listOf(
    Tier.JUNIOR to "bg-gray-400/20 text-gray-400",
    Tier.SENIOR to "bg-green-500/20 text-green-500",
    Tier.MASTER to "bg-orange-400/20 text-orange-400",
    Tier.GRANDMASTER to "bg-purple-500/20 text-purple-500",
)

private data class TrophyRow(
    val id: String,
    val iconPath: String,
    val contestName: String,
    val date: String,
    val tierLabel: String,
    val tierClass: String,
)

private val TROPHY_ROWS = listOf(
    TrophyRow("trophy-0", "icons/trophies/gold.svg", "Kotlin Open 2024", "Dec 2024", "Grandmaster", "text-yellow-400"),
    TrophyRow("trophy-1", "icons/trophies/silver.svg", "Android Challenge #7", "Nov 2024", "Master", "text-gray-300"),
    TrophyRow("trophy-2", "icons/trophies/bronze.svg", "Weekly Sprint #42", "Oct 2024", "Senior", "text-orange-400"),
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

            div(className = "mt-16 mb-4") {
                span(className = "text-xs font-semibold uppercase tracking-widest text-on-surface/40") {
                    +i18n.tr("Trophy Case")
                }
            }
            div(className = "flex flex-col gap-3") {
                TROPHY_ROWS.forEach { row -> trophyRow(row) }
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
        div(className = "relative h-3 bg-surface-variant rounded-full overflow-hidden") {
            div(
                id = "gamification-tier-fill",
                className = "absolute inset-y-0 start-0 rounded-full " +
                    "bg-gradient-to-r from-gray-400 via-green-500 via-orange-400 to-purple-500",
            ) {
                attribute("style", "width: 0%;")
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
        className = "badge-card relative h-44 cursor-pointer select-none rounded-2xl",
    ) {
        tabindex(0)
        role("button")
        attribute("aria-label", "${i18n.tr(badge.name)} — ${i18n.tr("press Enter to reveal unlock condition")}")
        attribute("style", "perspective: 700px;")

        // Inner flipper
        div(className = "relative w-full h-full") {
            attribute(
                "style",
                "transform-style: preserve-3d; transition: transform 0.4s ease; " +
                    "transform: rotateY(${if (flipped) 180 else 0}deg);",
            )

            // Front face
            div(
                className = "absolute inset-0 flex flex-col items-center justify-center gap-3 " +
                    "rounded-2xl border border-outline/20 bg-surface-container p-4",
            ) {
                attribute("style", "backface-visibility: hidden;")
                img(
                    src = "/${badge.iconPath}",
                    alt = i18n.tr(badge.name),
                    className = "w-12 h-12 object-contain",
                ) {}
                span(className = "text-xs font-semibold text-center text-on-surface/80 leading-tight") {
                    +i18n.tr(badge.name)
                }
            }

            // Back face
            div(
                className = "absolute inset-0 flex flex-col items-center justify-center gap-2 " +
                    "rounded-2xl border border-primary/30 bg-primary/10 p-4",
            ) {
                attribute("style", "backface-visibility: hidden; transform: rotateY(180deg);")
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

@Composable
private fun IComponent.trophyRow(row: TrophyRow) {
    div(
        id = row.id,
        className = "flex items-center gap-4 p-4 rounded-xl bg-surface-container " +
            "border border-outline/10 hover:border-outline/30 transition-colors duration-150",
    ) {
        img(src = "/${row.iconPath}", alt = row.contestName, className = "w-8 h-8 object-contain flex-shrink-0") {}
        div(className = "flex-1 min-w-0") {
            span(className = "block text-sm font-medium text-on-surface truncate") {
                +i18n.tr(row.contestName)
            }
            span(className = "block text-xs text-on-surface/40") { +row.date }
        }
        span(className = "text-xs font-semibold ${row.tierClass} flex-shrink-0") {
            +i18n.tr(row.tierLabel)
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

    TROPHY_ROWS.forEachIndexed { idx, row ->
        document.getElementById(row.id)?.let { el ->
            gsap.from(
                el,
                unsafeJso {
                    opacity = 0.0
                    y = 12.0
                    duration = TROPHY_ANIM_DURATION
                    delay = idx * TROPHY_STAGGER
                    ease = "power1.out"
                    scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                        trigger = el
                        start = SECTION_SCROLL_START
                        once = true
                    }
                },
            )
        }
    }
}
