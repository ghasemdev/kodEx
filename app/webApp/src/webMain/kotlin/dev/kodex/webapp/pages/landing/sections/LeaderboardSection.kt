@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod")

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
import dev.kodex.shared.session.SessionState
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.ScrollTriggerConfig
import dev.kodex.webapp.gsap.gsap
import dev.kodex.webapp.gsap.prefersReducedMotion
import dev.kodex.webapp.pages.landing.model.LeaderboardEntry
import dev.kodex.webapp.pages.landing.model.PlaceholderLeaderboard
import js.objects.unsafeJso
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.coroutines.delay

private const val SCORE_STEPS = 60
private const val SCORE_STEP_DELAY_MS = 27L
private const val LEADERBOARD_SCROLL_START = "top 85%"
private const val GUEST_PLACEHOLDER_RANK = 48
private const val GUEST_PLACEHOLDER_SCORE = 1240

private val TIER_ICON_PATH: Map<Tier, String> = mapOf(
    Tier.JUNIOR to "icons/tiers/junior.svg",
    Tier.SENIOR to "icons/tiers/senior.svg",
    Tier.MASTER to "icons/tiers/master.svg",
    Tier.GRANDMASTER to "icons/tiers/grandmaster.svg",
)

@Composable
fun IComponent.LeaderboardSection(session: SessionState = SessionState.Guest) {
    var scoresVisible by remember { mutableStateOf(false) }
    val targetScores = remember { PlaceholderLeaderboard.entries.map { it.score } }
    var displayScores by remember { mutableStateOf(List(PlaceholderLeaderboard.entries.size) { 0 }) }

    LaunchedEffect(Unit) { setupLeaderboardAnimation { scoresVisible = true } }

    LaunchedEffect(scoresVisible) {
        if (!scoresVisible) return@LaunchedEffect
        repeat(SCORE_STEPS) { step ->
            val ratio = (step + 1).toDouble() / SCORE_STEPS
            displayScores = targetScores.map { (it * ratio).toInt() }
            delay(SCORE_STEP_DELAY_MS.milliseconds)
        }
        displayScores = targetScores
    }

    div(id = "leaderboard-section", className = "py-24 bg-surface-container/30") {
        div(className = "container mx-auto px-4") {
            div(className = "flex items-end justify-between mb-8") {
                div {
                    h2(className = "text-3xl font-bold text-on-surface mb-1") {
                        +i18n.tr("Leaderboard")
                    }
                    p(className = "text-on-surface/60") {
                        +i18n.tr("Top performers this season.")
                    }
                }
                div(
                    className = "hidden md:flex px-3 py-1.5 text-sm cursor-pointer text-primary " +
                        "hover:bg-primary/10 rounded-lg transition-colors duration-150",
                ) {
                    role("link")
                    tabindex(0)
                    onKeydown { e -> if (e.key == "Enter" || e.key == " ") e.preventDefault() }
                    +i18n.tr("View full leaderboard →")
                }
            }

            leaderboardTable(PlaceholderLeaderboard.entries, displayScores)

            if (session is SessionState.Guest) {
                div(
                    className = "mt-4 flex items-center gap-4 p-4 rounded-xl " +
                        "border border-dashed border-primary/30 bg-primary/5",
                ) {
                    span(className = "text-sm text-on-surface/50 font-mono w-6 text-center") {
                        +"#$GUEST_PLACEHOLDER_RANK"
                    }
                    div(
                        className = "w-8 h-8 rounded-full flex items-center justify-center " +
                            "bg-surface-variant text-on-surface/40 text-xs font-bold",
                    ) { +"?" }
                    span(className = "flex-1 text-sm text-on-surface/50 italic") {
                        +i18n.tr("Sign up to claim your rank")
                    }
                    span(className = "text-sm font-mono text-on-surface/30") {
                        +"$GUEST_PLACEHOLDER_SCORE"
                    }
                }
            }

            div(className = "mt-6 flex md:hidden justify-center") {
                div(
                    className = "px-4 py-2 text-sm cursor-pointer text-primary " +
                        "hover:bg-primary/10 rounded-lg transition-colors duration-150",
                ) {
                    role("link")
                    tabindex(0)
                    onKeydown { e -> if (e.key == "Enter" || e.key == " ") e.preventDefault() }
                    +i18n.tr("View full leaderboard →")
                }
            }
        }
    }
}

@Composable
private fun IComponent.leaderboardTable(entries: List<LeaderboardEntry>, displayScores: List<Int>) {
    div(className = "rounded-2xl border border-outline/20 overflow-hidden") {
        div(
            className = "grid grid-cols-[2rem_1fr_auto] sm:grid-cols-[2rem_1fr_auto_auto_auto] " +
                "gap-3 px-4 py-2 bg-surface-variant/50 text-[10px] uppercase tracking-wider text-on-surface/40",
        ) {
            span { +"#" }
            span { +i18n.tr("User") }
            span(className = "text-center hidden sm:block") { +i18n.tr("Solved") }
            span(className = "text-center hidden sm:block") { +i18n.tr("Streak") }
            span(className = "text-end") { +i18n.tr("Score") }
        }
        entries.forEachIndexed { idx, entry ->
            leaderboardRow(entry, displayScores.getOrNull(idx) ?: 0, idx)
        }
    }
}

@Composable
private fun IComponent.leaderboardRow(entry: LeaderboardEntry, displayScore: Int, idx: Int) {
    val rowBg = if (idx == 0) "bg-primary/5" else ""
    div(
        id = "leaderboard-row-$idx",
        className = "grid grid-cols-[2rem_1fr_auto] sm:grid-cols-[2rem_1fr_auto_auto_auto] " +
            "gap-3 px-4 py-3 items-center border-t border-outline/10 $rowBg",
    ) {
        span(className = "text-sm font-mono text-on-surface/40 text-center") {
            +"${entry.rank}"
        }
        div(className = "flex items-center gap-2 min-w-0") {
            avatarCircle(entry.username)
            div(className = "min-w-0") {
                span(className = "block text-sm font-medium text-on-surface truncate") {
                    +entry.username
                }
                tierChip(entry.tier)
            }
        }
        span(className = "text-sm font-mono text-on-surface/60 text-center hidden sm:block") {
            +"${entry.solved}"
        }
        span(className = "text-sm font-mono text-on-surface/60 text-center hidden sm:block") {
            +"${entry.streakDays}d"
        }
        span(
            id = "leaderboard-score-$idx",
            className = "text-sm font-bold font-mono text-primary text-end tabular-nums",
        ) {
            +"$displayScore"
        }
    }
}

@Composable
private fun IComponent.avatarCircle(username: String) {
    val initial = username.take(1).uppercase()
    val hue = (username.hashCode() % 360 + 360) % 360
    div(
        className = "w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 " +
            "text-xs font-bold text-white",
    ) {
        attribute("style", "background: hsl($hue, 55%, 45%);")
        +initial
    }
}

@Composable
private fun IComponent.tierChip(tier: Tier) {
    val icon = TIER_ICON_PATH[tier] ?: return
    div(className = "flex items-center gap-1 mt-0.5") {
        img(src = "/$icon", alt = i18n.tr(tier.displayName), className = "w-3 h-3 object-contain") {}
        span(className = "text-[10px] text-on-surface/40") { +i18n.tr(tier.displayName) }
    }
}

private fun setupLeaderboardAnimation(onEnterViewport: () -> Unit) {
    if (prefersReducedMotion()) {
        onEnterViewport()
        return
    }
    val section = document.getElementById("leaderboard-section") ?: return
    gsap.from(
        section,
        unsafeJso {
            opacity = 0.0
            y = 16.0
            duration = 0.5
            ease = "power2.out"
            scrollTrigger = unsafeJso<ScrollTriggerConfig> {
                trigger = section
                start = LEADERBOARD_SCROLL_START
                once = true
                onEnter = { onEnterViewport() }
            }
        },
    )
}
