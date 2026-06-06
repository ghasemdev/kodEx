@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod")

package dev.kodex.webapp.pages.landing.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.kodex.webapp.gsap.prefersReducedMotion
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
    div(
        id = "badge-card-${badge.id}",
        className = "badge-card group relative h-52 select-none rounded-2xl overflow-hidden " +
            "border border-outline/20 bg-surface-container",
    ) {
        tabindex(0)
        role("img")
        attribute("aria-label", "${i18n.tr(badge.name)} — ${i18n.tr(badge.unlockCondition)}")

        img(
            src = "/${badge.iconPath}",
            alt = i18n.tr(badge.name),
            className = "w-full h-full object-contain p-2 transition-transform " +
                "duration-300 group-hover:scale-110 group-focus:scale-110",
        ) {}

        // Info overlay — slides up from bottom on hover/focus; backdrop-blur ensures
        // text stays readable regardless of the badge image colors beneath.
        div(
            className = "absolute inset-x-0 bottom-0 backdrop-blur-sm bg-surface-container-high/90 " +
                "translate-y-full group-hover:translate-y-0 group-focus-within:translate-y-0 " +
                "transition-transform duration-[400ms] ease-out p-3 pt-5",
        ) {
            span(className = "block text-xs font-semibold text-on-surface text-center mb-1") {
                +i18n.tr(badge.name)
            }
            span(className = "block text-[10px] text-on-surface/60 leading-snug text-center") {
                +i18n.tr(badge.unlockCondition)
            }
        }
    }
}

private fun animateGamification() {
    if (prefersReducedMotion()) return
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
