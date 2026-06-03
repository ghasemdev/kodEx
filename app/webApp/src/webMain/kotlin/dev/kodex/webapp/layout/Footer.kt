package dev.kodex.webapp.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.footer
import dev.kilua.html.p
import dev.kilua.html.span
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.gsap
import js.objects.unsafeJso
import kotlin.time.Clock
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val ICON_BOUNCE_SCALE = 1.15
private const val ICON_BOUNCE_DURATION = 0.3
private const val SOCIAL_ICON_PREFIX = "footer-social-"

private data class FooterLink(val labelKey: String, val url: String, val external: Boolean = false)

@Immutable
private data class FooterColumn(val titleKey: String, val links: List<FooterLink>)

private val FOOTER_COLUMNS = listOf(
    FooterColumn(
        titleKey = "Platform",
        links = listOf(
            FooterLink("Problems", "/problems"),
            FooterLink("Contests", "/contests"),
            FooterLink("Leaderboard", "/leaderboard"),
            FooterLink("Create Exam", "/exam/new"),
        ),
    ),
    FooterColumn(
        titleKey = "Community",
        links = listOf(
            FooterLink("GitHub", "https://github.com/kodex", external = true),
            FooterLink("Twitter / X", "https://twitter.com/kodex", external = true),
            FooterLink("Telegram", "https://t.me/kodex", external = true),
            FooterLink("Discord", "https://discord.gg/kodex", external = true),
        ),
    ),
    FooterColumn(
        titleKey = "Legal",
        links = listOf(
            FooterLink("About", "/about"),
            FooterLink("Contact", "/contact"),
            FooterLink("Privacy Policy", "/privacy"),
            FooterLink("Terms of Service", "/terms"),
            FooterLink("Cookies", "/cookies"),
        ),
    ),
)

private val SOCIAL_LINKS = listOf(
    Triple("gh", "fa-brands fa-github", "GitHub"),
    Triple("tw", "fa-brands fa-x-twitter", "Twitter / X"),
    Triple("tg", "fa-brands fa-telegram", "Telegram"),
    Triple("dc", "fa-brands fa-discord", "Discord"),
)

@Composable
fun IComponent.Footer() {
    footer(
        id = "site-footer",
        className = "border-t border-outline/20 bg-surface-container/40 py-12",
    ) {
        div(className = "container mx-auto px-4") {
            div(className = "grid grid-cols-1 md:grid-cols-3 gap-8 mb-10") {
                FOOTER_COLUMNS.forEach { column -> footerColumn(column) }
            }

            div(
                className = "flex flex-col md:flex-row items-center justify-between gap-4 " +
                    "pt-6 border-t border-outline/10",
            ) {
                p(className = "text-sm text-on-surface/40") {
                    +"© ${currentYear()} KodEx — ${i18n.tr("The Kotlin & Android Coding Arena")}"
                }
                div(className = "flex items-center gap-3") {
                    SOCIAL_LINKS.forEach { (key, icon, label) ->
                        socialIcon(id = "$SOCIAL_ICON_PREFIX$key", icon = icon, label = label)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { setupSocialHoverAnimations() }
}

@Composable
private fun IComponent.footerColumn(column: FooterColumn) {
    div {
        span(className = "text-xs font-semibold uppercase tracking-wider text-on-surface/40 mb-3 block") {
            +i18n.tr(column.titleKey)
        }
        div(className = "flex flex-col gap-2") {
            column.links.forEach { link ->
                div(
                    className = "text-sm text-on-surface/60 hover:text-on-surface cursor-pointer " +
                        "transition-colors duration-150 w-fit",
                ) {
                    role("link")
                    tabindex(0)
                    if (link.external) {
                        attribute("aria-label", "${i18n.tr(link.labelKey)} ${i18n.tr("(opens in new tab)")}")
                    }
                    +i18n.tr(link.labelKey)
                    onClick {
                        if (link.external) {
                            window.open(link.url, "_blank")
                        }
                    }
                    onKeydown { e ->
                        if ((e.key == "Enter" || e.key == " ") && link.external) {
                            window.open(link.url, "_blank")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IComponent.socialIcon(id: String, icon: String, label: String) {
    div(
        id = id,
        className = "w-8 h-8 flex items-center justify-center rounded-lg " +
            "text-on-surface/40 hover:text-on-surface bg-surface-container " +
            "hover:bg-surface-variant cursor-pointer transition-colors duration-150 " +
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50",
    ) {
        tabindex(0)
        role("link")
        attribute("aria-label", label)
        span(className = "$icon text-sm") {}
    }
}

@Suppress("LabeledExpression")
private fun setupSocialHoverAnimations() {
    SOCIAL_LINKS.forEach { (key, _, _) ->
        val element = document.getElementById("$SOCIAL_ICON_PREFIX$key") ?: return@forEach
        element.addEventListener("mouseenter", {
            gsap.to(
                element,
                unsafeJso {
                    scale = ICON_BOUNCE_SCALE
                    duration = ICON_BOUNCE_DURATION
                    ease = "elastic.out(1,0.5)"
                },
            )
        })
        element.addEventListener("mouseleave", {
            gsap.to(
                element,
                unsafeJso {
                    scale = 1.0
                    duration = ICON_BOUNCE_DURATION
                    ease = "power2.out"
                },
            )
        })
    }
}

private fun currentYear(): String = Clock.System.now()
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .year
    .toString()
