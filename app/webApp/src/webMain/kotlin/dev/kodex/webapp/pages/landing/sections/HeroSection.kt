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
import dev.kilua.html.h1
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.shared.landing.LandingStats
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.gsap
import js.objects.unsafeJso
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.coroutines.delay

@Suppress("StringTemplateIndent")
private val KOTLIN_CODE = """
    fun solution(input: String): String {
        return input
            .trim()
            .uppercase()
    }
""".trimIndent()

private val TYPING_DELAY = 38.milliseconds
private val INITIAL_PAUSE = 600.milliseconds
private val COMPILE_DELAY = 900.milliseconds
private val LOOP_PAUSE = 2800.milliseconds
private val COUNTER_STEP_DELAY = 30.milliseconds
private val TEST_ROW_DELAY = 320.milliseconds

private const val COUNTER_STEPS = 60
private const val BLOB_ANIM_DURATION = 10.0
private const val BLOB2_ANIM_DURATION = 12.0

@Composable
fun IComponent.StatCounter(label: String, value: Int?, error: Boolean = false) {
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
    var typedChars by remember { mutableStateOf(0) }
    var showCompileStatus by remember { mutableStateOf(false) }
    var showTestRows by remember { mutableStateOf(false) }
    var problemsDisplay by remember { mutableStateOf<Int?>(null) }
    var usersDisplay by remember { mutableStateOf<Int?>(null) }
    var contestsDisplay by remember { mutableStateOf<Int?>(null) }

    // Infinite typing loop — restarts automatically after each cycle
    LaunchedEffect(Unit) {
        while (true) {
            typedChars = 0
            showCompileStatus = false
            showTestRows = false
            delay(INITIAL_PAUSE)
            repeat(KOTLIN_CODE.length) {
                typedChars++
                delay(TYPING_DELAY)
            }
            showCompileStatus = true
            delay(COMPILE_DELAY)
            showTestRows = true
            delay(LOOP_PAUSE)
        }
    }

    // GSAP compile bar scaleX 0→1
    LaunchedEffect(showCompileStatus) {
        if (!showCompileStatus) return@LaunchedEffect
        val element = document.getElementById("hero-compile-bar") ?: return@LaunchedEffect
        gsap.to(
            element,
            unsafeJso {
                scaleX = 1.0
                duration = 0.8
                ease = "power2.out"
            },
        )
    }

    // GSAP test row slide-in
    LaunchedEffect(showTestRows) {
        if (!showTestRows) return@LaunchedEffect
        for (rowIdx in 1..3) {
            val element = document.getElementById("hero-test-row-$rowIdx") ?: continue
            gsap.from(
                element,
                unsafeJso {
                    x = -16.0
                    opacity = 0.0
                    duration = 0.4
                    ease = "power2.out"
                },
            )
            delay(TEST_ROW_DELAY)
        }
    }

    // Stats counter roll-up (no innerHTML — Compose state only)
    LaunchedEffect(statsState) {
        if (statsState !is UiState.Success) return@LaunchedEffect
        val stats = statsState.data
        repeat(COUNTER_STEPS) { step ->
            val ratio = (step + 1).toDouble() / COUNTER_STEPS
            problemsDisplay = (stats.totalProblems * ratio).toInt()
            usersDisplay = (stats.totalUsers * ratio).toInt()
            contestsDisplay = (stats.totalContests * ratio).toInt()
            delay(COUNTER_STEP_DELAY)
        }
        problemsDisplay = stats.totalProblems
        usersDisplay = stats.totalUsers
        contestsDisplay = stats.totalContests
    }

    // Ambient blob animations
    LaunchedEffect(Unit) {
        document.getElementById("hero-blob-1")?.let { el ->
            gsap.to(
                el,
                unsafeJso {
                    x = 80.0
                    y = -40.0
                    duration = BLOB_ANIM_DURATION
                    repeat = -1
                    yoyo = true
                    ease = "sine.inOut"
                },
            )
        }
        document.getElementById("hero-blob-2")?.let { el ->
            gsap.to(
                el,
                unsafeJso {
                    x = -60.0
                    y = 50.0
                    duration = BLOB2_ANIM_DURATION
                    repeat = -1
                    yoyo = true
                    ease = "sine.inOut"
                },
            )
        }
    }

    div(className = "relative overflow-hidden") {
        div(
            id = "hero-blob-1",
            className = "absolute -top-40 -start-40 w-96 h-96 rounded-full bg-primary/20 blur-3xl pointer-events-none",
        ) {}
        div(
            id = "hero-blob-2",
            className = "absolute -bottom-40 -end-40 w-80 h-80 rounded-full " +
                "bg-secondary/15 blur-3xl pointer-events-none",
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
                div(className = "flex flex-wrap gap-8 mt-4 pt-6 border-t border-outline/20 w-full") {
                    buildStatCounters(statsState, problemsDisplay, usersDisplay, contestsDisplay)
                }
            }

            // Right column — Mac-style code editor panel
            div(
                id = "hero-editor-panel",
                className = "flex-1 w-full max-w-lg rounded-2xl border border-outline/20 " +
                    "bg-neutral-900 overflow-hidden shadow-2xl",
            ) {
                macWindowBar()
                editorContent(code = KOTLIN_CODE, typedChars = typedChars)
                terminalArea(showCompileStatus = showCompileStatus, showTestRows = showTestRows)
            }
        }
    }
}

@Composable
private fun IComponent.macWindowBar() {
    div(
        className = "flex items-center gap-2 px-4 py-3 bg-neutral-800 border-b border-white/5",
    ) {
        div(className = "w-3 h-3 rounded-full bg-[#FF5F57] mac-dot") {}
        div(className = "w-3 h-3 rounded-full bg-[#FEBC2E] mac-dot") {}
        div(className = "w-3 h-3 rounded-full bg-[#28C840] mac-dot") {}
        span(className = "ms-3 text-xs text-neutral-500 font-mono") { +"Solution.kt" }
    }
}

@Composable
private fun IComponent.editorContent(code: String, typedChars: Int) {
    div(
        className = "p-5 font-mono text-sm bg-neutral-900 h-52 overflow-hidden " +
            "text-neutral-200 leading-relaxed",
    ) {
        span(className = "whitespace-pre-wrap") { +code.take(typedChars) }
        span(className = "text-primary animate-pulse") { +"|" }
    }
}

@Composable
private fun IComponent.terminalArea(showCompileStatus: Boolean, showTestRows: Boolean) {
    div(
        className = "h-44 border-t border-white/5 bg-neutral-950 overflow-hidden",
    ) {
        div(className = "px-4 pt-3 pb-2 flex flex-col gap-1.5 h-full") {
            // Prompt line — always visible
            span(className = "text-xs font-mono text-green-400/70") {
                +"$ ./gradlew :test --tests \"SolutionTest\""
            }

            if (showCompileStatus) {
                // Status line
                span(className = "text-xs font-mono text-neutral-400") {
                    +if (showTestRows) "BUILD SUCCESSFUL in 1s" else "Compiling..."
                }
                // Progress bar
                div(className = "relative h-0.5 bg-neutral-800 rounded-full overflow-hidden") {
                    div(
                        id = "hero-compile-bar",
                        className = "absolute inset-0 bg-primary rounded-full origin-left",
                    ) {
                        attribute("style", "transform: scaleX(0);")
                    }
                }
                // Test result rows
                if (showTestRows) {
                    val testLines = listOf(
                        "  PASS  Test 1 passed in 9ms",
                        "  PASS  Test 2 passed in 12ms",
                        "  PASS  Test 3 passed in 8ms",
                    )
                    testLines.forEachIndexed { idx, text ->
                        div(
                            id = "hero-test-row-${idx + 1}",
                            className = "text-xs font-mono text-green-400",
                        ) {
                            +text
                        }
                    }
                }
            }
        }
    }
}

private const val PROBLEMS = "problems"
private const val DEVELOPERS = "developers"
private const val CONTESTS = "contests"

@Composable
private fun IComponent.buildStatCounters(
    statsState: UiState<LandingStats>,
    problemsDisplay: Int?,
    usersDisplay: Int?,
    contestsDisplay: Int?,
) {
    when (statsState) {
        is UiState.Error -> {
            StatCounter(label = i18n.tr(PROBLEMS), value = null, error = true)
            StatCounter(label = i18n.tr(DEVELOPERS), value = null, error = true)
            StatCounter(label = i18n.tr(CONTESTS), value = null, error = true)
        }

        is UiState.Success if problemsDisplay != null -> {
            StatCounter(label = i18n.tr(PROBLEMS), value = problemsDisplay)
            StatCounter(label = i18n.tr(DEVELOPERS), value = usersDisplay)
            StatCounter(label = i18n.tr(CONTESTS), value = contestsDisplay)
        }

        else -> {
            StatCounter(label = i18n.tr(PROBLEMS), value = null)
            StatCounter(label = i18n.tr(DEVELOPERS), value = null)
            StatCounter(label = i18n.tr(CONTESTS), value = null)
        }
    }
}
