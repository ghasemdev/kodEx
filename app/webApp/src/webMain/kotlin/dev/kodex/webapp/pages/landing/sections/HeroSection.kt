@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod", "CognitiveComplexMethod")

package dev.kodex.webapp.pages.landing.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.button
import dev.kilua.html.div
import dev.kilua.html.h1
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.GsapVars
import dev.kodex.webapp.gsap.gsap
import js.objects.unsafeJso
import kotlinx.browser.document
import kotlinx.coroutines.delay

private const val KOTLIN_CODE =
    "fun solution(\n  input: String\n): String {\n  return input\n    .trim()\n    .uppercase()\n}"

private const val ANDROID_CODE =
    "class TaskViewModel(\n  app: Application\n) : AndroidViewModel(app) {\n  fun onDone() {\n    _state.update {\n      it.copy(done = true)\n    }\n  }\n}"

private const val TYPING_DELAY_MS = 38L
private const val INITIAL_PAUSE_MS = 500L
private const val COMPILE_DELAY_MS = 900L
private const val LOOP_PAUSE_MS = 2500L
private const val COUNTER_STEPS = 60
private const val COUNTER_STEP_DELAY_MS = 30L
private const val TEST_ROW_DELAY_MS = 320L
private const val BLOB_ANIM_DURATION = 10.0
private const val BLOB2_ANIM_DURATION = 12.0

enum class HeroTab(val displayLabel: String) {
    Kotlin("⬛ Kotlin"),
    Android("🤖 Android"),
}

@Composable
fun IComponent.StatCounter(label: String, value: Int?, error: Boolean = false,) {
    div(className = "flex flex-col items-center gap-1") {
        when {
            error -> span(className = "stat-value text-4xl font-bold text-on-surface/40") { +"—" }
            value == null -> div(className = "stat-shimmer animate-pulse bg-surface-variant rounded-lg h-10 w-20") {}
            else -> span(className = "stat-value text-4xl font-bold tabular-nums text-primary") {
                +value.toString()
            }
        }
        span(className = "stat-label text-xs uppercase tracking-widest text-on-surface/50 mt-0.5") {
            +label
        }
    }
}

@Composable
fun IComponent.HeroSection(statsState: UiState<LandingStats>) {
    var activeTab by remember { mutableStateOf(HeroTab.Kotlin) }
    var typedChars by remember { mutableStateOf(0) }
    var showCompileStatus by remember { mutableStateOf(false) }
    var showTestRows by remember { mutableStateOf(false) }
    var problemsDisplay by remember { mutableStateOf<Int?>(null) }
    var usersDisplay by remember { mutableStateOf<Int?>(null) }
    var contestsDisplay by remember { mutableStateOf<Int?>(null) }

    val currentCode = if (activeTab == HeroTab.Kotlin) KOTLIN_CODE else ANDROID_CODE

    // Typing animation — restarts on each tab change; auto-switches tab when done
    LaunchedEffect(activeTab) {
        typedChars = 0
        showCompileStatus = false
        showTestRows = false
        delay(INITIAL_PAUSE_MS)
        repeat(currentCode.length) {
            typedChars++
            delay(TYPING_DELAY_MS)
        }
        showCompileStatus = true
        delay(COMPILE_DELAY_MS)
        showTestRows = true
        delay(LOOP_PAUSE_MS)
        activeTab = if (activeTab == HeroTab.Kotlin) HeroTab.Android else HeroTab.Kotlin
    }

    // GSAP compile bar fill (scaleX 0→1)
    LaunchedEffect(showCompileStatus) {
        if (!showCompileStatus) return@LaunchedEffect
        val el = document.getElementById("hero-compile-bar") ?: return@LaunchedEffect
        gsap.to(el, unsafeJso<GsapVars> {
            scaleX = 1.0;
            duration = 0.8;
            ease = "power2.out"
        })
    }

    // GSAP test row slide-in — keyed on both flags to re-run after tab switch
    val tabKey = activeTab.name.lowercase()
    LaunchedEffect(showTestRows, tabKey) {
        if (!showTestRows) return@LaunchedEffect
        for (rowIdx in 1..3) {
            val el = document.getElementById("hero-test-row-$tabKey-$rowIdx") ?: continue
            gsap.from(el, unsafeJso<GsapVars> {
                x = -16.0;
                opacity = 0.0;
                duration = 0.4;
                ease = "power2.out"
            })
            delay(TEST_ROW_DELAY_MS)
        }
    }

    // Stats counter roll-up via Compose state (no innerHTML — uses text node rendering)
    LaunchedEffect(statsState) {
        if (statsState !is UiState.Success) return@LaunchedEffect
        val stats = statsState.data
        repeat(COUNTER_STEPS) { step ->
            val ratio = (step + 1).toDouble() / COUNTER_STEPS
            problemsDisplay = (stats.totalProblems * ratio).toInt()
            usersDisplay = (stats.totalUsers * ratio).toInt()
            contestsDisplay = (stats.totalContests * ratio).toInt()
            delay(COUNTER_STEP_DELAY_MS)
        }
        problemsDisplay = stats.totalProblems
        usersDisplay = stats.totalUsers
        contestsDisplay = stats.totalContests
    }

    // Ambient background blob animations
    LaunchedEffect(Unit) {
        document.getElementById("hero-blob-1")?.let { el ->
            gsap.to(
                el,
                unsafeJso<GsapVars> {
                    x = 80.0;
                    y = -40.0;
                    duration = BLOB_ANIM_DURATION;
                    repeat = -1;
                    yoyo = true;
                    ease = "sine.inOut"
                },
            )
        }
        document.getElementById("hero-blob-2")?.let { el ->
            gsap.to(
                el,
                unsafeJso<GsapVars> {
                    x = -60.0;
                    y = 50.0;
                    duration = BLOB2_ANIM_DURATION;
                    repeat = -1;
                    yoyo = true;
                    ease = "sine.inOut"
                },
            )
        }
    }

    div(className = "relative overflow-hidden") {
        // Ambient gradient mesh background
        div(
            id = "hero-blob-1",
            className = "absolute -top-40 -start-40 w-96 h-96 rounded-full bg-primary/20 blur-3xl pointer-events-none",
        ) {}
        div(
            id = "hero-blob-2",
            className = "absolute -bottom-40 -end-40 w-80 h-80 rounded-full bg-secondary/15 blur-3xl pointer-events-none",
        ) {}

        div(
            className = "relative container mx-auto px-4 py-24 flex flex-col lg:flex-row " +
                "items-center gap-12 min-h-screen",
        ) {
            // Left column — headline, tagline, CTAs, stats
            div(className = "flex-1 flex flex-col items-start gap-6") {
                h1(
                    className = "text-5xl lg:text-6xl font-extrabold leading-tight text-on-surface tracking-tight",
                ) {
                    +i18n.tr("KodEx — The Kotlin & Android Coding Arena")
                }
                p(className = "text-xl text-on-surface/70 max-w-lg") {
                    +i18n.tr("Compete. Build. Master.")
                }
                div(className = "flex flex-wrap gap-3 mt-2") {
                    Button(label = i18n.tr("Get Started"), variant = ButtonVariant.Primary)
                    Button(label = i18n.tr("Explore Problems"), variant = ButtonVariant.Secondary)
                }

                // Stats row
                div(className = "flex flex-wrap gap-8 mt-4 pt-6 border-t border-outline/20 w-full") {
                    buildStatCounters(statsState, problemsDisplay, usersDisplay, contestsDisplay)
                }
            }

            // Right column — tabbed code editor panel
            div(
                className = "flex-1 w-full max-w-lg rounded-2xl border border-outline/20 " +
                    "bg-surface-container overflow-hidden shadow-2xl",
            ) {
                editorTabBar(activeTab = activeTab, onTabClick = { activeTab = it })
                editorContent(code = currentCode, typedChars = typedChars)
                compileStatusArea(showCompileStatus = showCompileStatus, showTestRows = showTestRows)
                if (showTestRows) {
                    testRows(activeTab = activeTab, tabKey = tabKey)
                }
            }
        }
    }
}

@Composable
private fun IComponent.buildStatCounters(
    statsState: UiState<LandingStats>,
    problemsDisplay: Int?,
    usersDisplay: Int?,
    contestsDisplay: Int?,
) {
    when {
        statsState is UiState.Error -> {
            StatCounter(label = i18n.tr("problems"), value = null, error = true)
            StatCounter(label = i18n.tr("developers"), value = null, error = true)
            StatCounter(label = i18n.tr("contests"), value = null, error = true)
        }

        statsState is UiState.Success && problemsDisplay != null -> {
            StatCounter(label = i18n.tr("problems"), value = problemsDisplay)
            StatCounter(label = i18n.tr("developers"), value = usersDisplay)
            StatCounter(label = i18n.tr("contests"), value = contestsDisplay)
        }

        else -> {
            StatCounter(label = i18n.tr("problems"), value = null)
            StatCounter(label = i18n.tr("developers"), value = null)
            StatCounter(label = i18n.tr("contests"), value = null)
        }
    }
}

@Composable
private fun IComponent.editorTabBar(activeTab: HeroTab, onTabClick: (HeroTab) -> Unit) {
    div(className = "flex border-b border-outline/20") {
        HeroTab.entries.forEach { tab ->
            val isActive = tab == activeTab
            button(
                id = "hero-tab-${tab.name.lowercase()}",
                className = "flex-1 py-3 px-4 text-sm font-medium transition-all duration-150 " +
                    if (isActive) {
                        "text-primary border-b-2 border-primary bg-primary/5 tab-active"
                    } else {
                        "text-on-surface/60 hover:text-on-surface hover:bg-surface-variant"
                    },
            ) {
                attribute("aria-selected", isActive.toString())
                +tab.displayLabel
                onClick { onTabClick(tab) }
            }
        }
    }
}

@Composable
private fun IComponent.editorContent(code: String, typedChars: Int) {
    div(
        className = "p-4 font-mono text-sm bg-neutral-900 dark:bg-neutral-950 min-h-48 " +
            "text-neutral-200 leading-relaxed",
    ) {
        span(className = "whitespace-pre-wrap") { +code.take(typedChars) }
        span(className = "text-primary animate-pulse") { +"|" }
    }
}

@Composable
private fun IComponent.compileStatusArea(showCompileStatus: Boolean, showTestRows: Boolean) {
    div(className = "px-4 py-2 border-t border-outline/20 text-xs min-h-8") {
        if (showCompileStatus) {
            div(className = "flex flex-col gap-1.5") {
                span(className = "text-on-surface/60") {
                    +if (showTestRows) "✓ Build successful" else "▶ Compiling..."
                }
                div(className = "relative h-1 bg-surface-variant rounded-full overflow-hidden") {
                    div(
                        id = "hero-compile-bar",
                        className = "absolute inset-0 bg-primary rounded-full origin-left",
                    ) {
                        // scaleX starts at 0; GSAP animates it to 1
                        attribute("style", "transform: scaleX(0);")
                    }
                }
            }
        }
    }
}

@Composable
private fun IComponent.testRows(activeTab: HeroTab, tabKey: String) {
    val rows = if (activeTab == HeroTab.Kotlin) {
        listOf(
            "✓  Test 1 — 9ms   PASS",
            "✓  Test 2 — 12ms  PASS",
            "✓  Test 3 — 8ms   PASS  🎉",
        )
    } else {
        listOf(
            "✓  onClick handler  PASS",
            "✓  ViewModel state  PASS",
            "✓  UI assertion     PASS  🎉",
        )
    }
    div(className = "px-4 pb-4 flex flex-col gap-1") {
        rows.forEachIndexed { idx, text ->
            div(
                id = "hero-test-row-$tabKey-${idx + 1}",
                className = "text-xs font-mono text-success",
            ) {
                +text
            }
        }
    }
}
