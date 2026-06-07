@file:Suppress("MagicNumber", "LabeledExpression", "LongMethod", "CognitiveComplexMethod", "StringLiteralDuplication")

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
import kotlinx.browser.window
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
private const val ICON_APPEAR_DURATION = 0.24

private const val MAC_DOT_RED = "#FF5F57"
private const val MAC_DOT_YELLOW = "#FEBC2E"
private const val MAC_DOT_GREEN = "#28C840"

// inset-0 + own flex: 'absolute' children don't join parent flex layout,
// so they need inset-0 + flex to visually center inside the dot circle.
private const val DOT_SYMBOL_CLASSES =
    "absolute inset-0 flex items-center justify-center " +
        "text-[9px] font-black leading-none select-none " +
        "opacity-0 group-hover:opacity-100 transition-opacity text-black/80"

private const val PROBLEMS = "problems"
private const val DEVELOPERS = "developers"
private const val CONTESTS = "contests"
private const val KEY_ENTER = "Enter"
private const val KEY_SPACE = " "

private enum class PanelState { Normal, TerminalClosed, Minimized, Maximized }

// ── Public StatCounter ────────────────────────────────────────────────────────

@Composable
fun IComponent.StatCounter(label: String, value: Int?, targetValue: Int? = null, error: Boolean = false) {
    div(className = "flex flex-col items-center gap-1 min-w-[5rem]") {
        div(className = "h-10 flex items-center justify-center") {
            when {
                error -> {
                    span(className = "stat-value text-4xl font-bold text-on-surface/40") { +"—" }
                }

                value == null -> {
                    div(
                        className = "stat-shimmer animate-pulse bg-surface-variant rounded-lg h-8 w-16",
                    ) {}
                }

                else -> {
                    // Reserve width for the final digit count up-front so the span never
                    // grows during the counting animation (tabular-nums makes each digit
                    // equal width; ch unit anchors to the "0" glyph at the current font size).
                    val digitCount = (targetValue ?: value).toString().length
                    span(
                        className = "stat-value text-4xl font-bold " +
                            "tabular-nums text-primary text-center inline-block",
                    ) {
                        attribute("style", "min-width: ${digitCount}ch;")
                        +value.toString()
                    }
                }
            }
        }
        span(className = "stat-label text-xs uppercase tracking-widest text-on-surface/50") {
            +label
        }
    }
}

// ── HeroSection ───────────────────────────────────────────────────────────────

@Composable
fun IComponent.HeroSection(statsState: UiState<LandingStats>) {
    var typedChars by remember { mutableStateOf(0) }
    var showCompileStatus by remember { mutableStateOf(false) }
    var showTestRows by remember { mutableStateOf(false) }
    var visibleRows by remember { mutableStateOf(0) }
    var problemsDisplay by remember { mutableStateOf<Int?>(null) }
    var usersDisplay by remember { mutableStateOf<Int?>(null) }
    var contestsDisplay by remember { mutableStateOf<Int?>(null) }
    var panelState by remember { mutableStateOf(PanelState.Normal) }

    LaunchedEffect(Unit) {
        while (true) {
            typedChars = 0
            showCompileStatus = false
            showTestRows = false
            visibleRows = 0
            delay(INITIAL_PAUSE)
            repeat(KOTLIN_CODE.length) {
                typedChars++
                delay(TYPING_DELAY)
            }
            showCompileStatus = true
            delay(COMPILE_DELAY)
            showTestRows = true
            for (i in 1..3) {
                visibleRows = i
                delay(TEST_ROW_DELAY)
            }
            delay(LOOP_PAUSE)
        }
    }

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

    LaunchedEffect(visibleRows) {
        if (visibleRows == 0) return@LaunchedEffect
        val element = document.getElementById("hero-test-row-$visibleRows") ?: return@LaunchedEffect
        gsap.from(
            element,
            unsafeJso {
                opacity = 0.0
                y = 6.0
                duration = 0.45
                ease = "power1.out"
            },
        )
    }

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

    LaunchedEffect(Unit) {
        document.getElementById("hero-blob-1")?.let { blob ->
            gsap.to(
                blob,
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
        document.getElementById("hero-blob-2")?.let { blob ->
            gsap.to(
                blob,
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

    // Spring-in animation for the terminal icon when it becomes visible.
    // The panel's opacity-0→1 cross-fade is handled by CSS transition (no GSAP needed there).
    LaunchedEffect(panelState) {
        if (panelState != PanelState.TerminalClosed) return@LaunchedEffect
        val prefersReducedMotion = runCatching {
            window.matchMedia("(prefers-reduced-motion: reduce)").matches
        }.getOrDefault(false)
        if (prefersReducedMotion) return@LaunchedEffect
        delay(16.milliseconds) // one frame — let Compose render the icon before GSAP reads the element
        val icon = document.getElementById("hero-terminal-icon") ?: return@LaunchedEffect
        gsap.from(
            icon,
            unsafeJso {
                scale = 0.4
                opacity = 0.0
                duration = ICON_APPEAR_DURATION
                ease = "back.out(1.7)"
            },
        )
    }

    div(id = "hero-section", className = "relative overflow-hidden") {
        div(
            id = "hero-blob-1",
            className = "absolute -top-40 -start-40 w-96 h-96 rounded-full bg-primary/20 blur-3xl pointer-events-none",
        ) {}
        div(
            id = "hero-blob-2",
            className = "absolute -bottom-40 -end-40 w-80 h-80 " +
                "rounded-full bg-secondary/15 blur-3xl pointer-events-none",
        ) {}

        div(
            className = "relative container mx-auto px-4 py-24 flex flex-col lg:flex-row " +
                "items-center gap-12 min-h-screen",
        ) {
            // Left column
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

            // Right column — wrapper keeps SAME dimensions in ALL panel states.
            // Both the panel and the terminal icon live here; CSS opacity cross-fades between them.
            // This avoids DOM replacement so CSS transitions actually fire between states.
            // The wrapper's lg:flex-1 max-w-lg is NEVER removed, so the left column width is stable
            // and the headline never reflows when pressing red/green.
            div(className = "relative lg:flex-1 w-full max-w-lg") {
                // Editor panel — always in DOM; fades out via CSS when TerminalClosed
                val panelVisClass = if (panelState == PanelState.TerminalClosed) {
                    "opacity-0 pointer-events-none"
                } else {
                    "opacity-100 pointer-events-auto"
                }
                div(
                    id = "hero-editor-panel",
                    className = "w-full rounded-2xl border border-outline/20 bg-neutral-900 " +
                        "overflow-clip shadow-2xl transition-all duration-300 $panelVisClass",
                ) {
                    attribute("dir", "ltr")
                    macWindowBar(panelState = panelState) { panelState = it }

                    // TerminalClosed included: prevents content expanding during panel opacity-out
                    val editorWrapClass = "overflow-hidden transition-all duration-300 ease-in-out " +
                        if (panelState == PanelState.Minimized || panelState == PanelState.TerminalClosed) {
                            "max-h-0 opacity-0"
                        } else {
                            "max-h-[420px] opacity-100"
                        }
                    div(id = "hero-panel-content", className = editorWrapClass) {
                        editorContent(code = KOTLIN_CODE, typedChars = typedChars)

                        // Inner CSS transition: collapses only terminal when TerminalClosed/Minimized
                        val termWrapClass = "overflow-hidden transition-all duration-300 ease-in-out " +
                            if (panelState == PanelState.Normal || panelState == PanelState.Maximized) {
                                "max-h-[200px] opacity-100"
                            } else {
                                "max-h-0 opacity-0"
                            }
                        div(id = "hero-panel-terminal", className = termWrapClass) {
                            terminalArea(
                                showCompileStatus = showCompileStatus,
                                showTestRows = showTestRows,
                                visibleRows = visibleRows,
                            )
                        }
                    }
                }

                // Terminal icon overlay — absolute so it doesn't push layout.
                // Fades in via CSS when TerminalClosed; GSAP adds the spring scale on top.
                val iconVisClass = if (panelState == PanelState.TerminalClosed) {
                    "opacity-100 pointer-events-auto"
                } else {
                    "opacity-0 pointer-events-none"
                }
                div(
                    className = "absolute inset-0 flex items-center justify-center " +
                        "transition-opacity duration-300 $iconVisClass",
                ) {
                    terminalAppIcon { panelState = PanelState.Normal }
                }
            }
        }
    }
}

// ── Mac window chrome ─────────────────────────────────────────────────────────

@Composable
private fun IComponent.macWindowBar(panelState: PanelState, onStateChange: (PanelState) -> Unit) {
    div(className = "group flex items-center gap-2 px-4 py-3 bg-neutral-800 border-b border-white/5") {
        macDot(color = MAC_DOT_RED, symbol = "×", ariaLabel = "Close to terminal icon") {
            onStateChange(PanelState.TerminalClosed)
        }
        macDot(color = MAC_DOT_YELLOW, symbol = "−", ariaLabel = "Minimize editor") {
            onStateChange(if (panelState == PanelState.Minimized) PanelState.Normal else PanelState.Minimized)
        }
        macDot(color = MAC_DOT_GREEN, symbol = "⊕", ariaLabel = "Maximize editor") {
            onStateChange(if (panelState == PanelState.Maximized) PanelState.Normal else PanelState.Maximized)
        }
        span(className = "ms-3 text-xs text-neutral-500 font-mono") { +"Solution.kt" }
        if (panelState != PanelState.Normal) {
            span(className = "ms-auto me-1 text-[10px] text-neutral-600 font-mono italic") {
                +when (panelState) {
                    PanelState.Minimized -> "— minimized"
                    PanelState.Maximized -> "— maximized"
                    PanelState.TerminalClosed -> "— closed"
                    PanelState.Normal -> ""
                }
            }
        }
    }
}

@Composable
private fun IComponent.macDot(color: String, symbol: String, ariaLabel: String, onClick: () -> Unit) {
    div(
        className = "mac-dot relative w-3.5 h-3.5 rounded-full cursor-pointer flex items-center " +
            "justify-center transition-transform hover:scale-125 active:scale-90",
    ) {
        attribute("style", "background-color: $color;")
        tabindex(0)
        role("button")
        attribute("aria-label", ariaLabel)
        span(className = DOT_SYMBOL_CLASSES) { +symbol }
        onClick { onClick() }
        onKeydown { e -> if (e.key == KEY_ENTER || e.key == KEY_SPACE) onClick() }
    }
}

// macOS Terminal.app dock icon — clicking reopens the full panel
@Composable
private fun IComponent.terminalAppIcon(onOpen: () -> Unit) {
    div(
        id = "hero-terminal-icon",
        className = "cursor-pointer select-none flex flex-col items-center justify-center gap-2 " +
            "w-24 h-24 rounded-2xl bg-[#1a1a1a] border border-neutral-800 shadow-2xl " +
            "hover:border-green-500/30 hover:shadow-green-500/10 " +
            "hover:scale-105 active:scale-95 transition-all duration-200",
    ) {
        tabindex(0)
        role("button")
        attribute("aria-label", i18n.tr("Reopen editor"))
        div(className = "flex items-baseline gap-px") {
            span(className = "text-green-400 font-mono text-xl font-bold leading-none") { +">" }
            span(className = "text-green-400 font-mono text-lg font-bold leading-none animate-pulse") { +"_" }
        }
        span(className = "text-neutral-600 text-[9px] font-mono tracking-widest uppercase") { +"terminal" }
        onClick { onOpen() }
        onKeydown { e -> if (e.key == KEY_ENTER || e.key == KEY_SPACE) onOpen() }
    }
}

// ── Editor sections ───────────────────────────────────────────────────────────

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
private fun IComponent.terminalArea(showCompileStatus: Boolean, showTestRows: Boolean, visibleRows: Int) {
    div(className = "h-44 border-t border-white/5 bg-neutral-950") {
        div(className = "px-4 pt-3 pb-2 flex flex-col gap-1.5 h-full") {
            span(className = "text-xs font-mono text-green-400/70") {
                +"$ ./gradlew :test --tests \"SolutionTest\""
            }
            if (showCompileStatus) {
                span(className = "text-xs font-mono text-neutral-400") {
                    +if (showTestRows) "BUILD SUCCESSFUL in 1s" else "Compiling..."
                }
                div(className = "relative h-0.5 bg-neutral-800 rounded-full overflow-hidden") {
                    div(
                        id = "hero-compile-bar",
                        className = "absolute inset-0 bg-primary rounded-full origin-left",
                    ) {
                        attribute("style", "transform: scaleX(0);")
                    }
                }
                val testLines = listOf(
                    "  PASS  Test 1 passed in 9ms",
                    "  PASS  Test 2 passed in 12ms",
                    "  PASS  Test 3 passed in 8ms",
                )
                testLines.take(visibleRows).forEachIndexed { idx, text ->
                    div(
                        id = "hero-test-row-${idx + 1}",
                        className = "text-xs font-mono text-green-400" + if (idx == 0) " mt-2" else "",
                    ) { +text }
                }
            }
        }
    }
}

// ── Stat counters ─────────────────────────────────────────────────────────────

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
            val data = statsState.data
            StatCounter(label = i18n.tr(PROBLEMS), value = problemsDisplay, targetValue = data.totalProblems)
            StatCounter(label = i18n.tr(DEVELOPERS), value = usersDisplay, targetValue = data.totalUsers)
            StatCounter(label = i18n.tr(CONTESTS), value = contestsDisplay, targetValue = data.totalContests)
        }

        else -> {
            StatCounter(label = i18n.tr(PROBLEMS), value = null)
            StatCounter(label = i18n.tr(DEVELOPERS), value = null)
            StatCounter(label = i18n.tr(CONTESTS), value = null)
        }
    }
}
