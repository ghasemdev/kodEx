@file:Suppress("LongMethod")

package dev.kodex.webapp.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.nav
import dev.kilua.html.span
import dev.kodex.shared.session.Plan
import dev.kodex.shared.session.SessionState
import dev.kodex.shared.session.UserRole
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.components.LanguageSwitcher
import dev.kodex.webapp.design.components.ThemeSwitcher
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.gsap.gsap
import js.objects.unsafeJso
import kotlinx.browser.document

@Composable
fun IComponent.GlobalNavBar(session: SessionState = SessionState.Guest) {
    val canCreateExam = session is SessionState.Authenticated &&
        session.roles.any { it in CREATOR_ROLES }
    var dropdownOpen by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }

    nav(
        id = "global-nav",
        className = "sticky top-0 z-50 bg-surface/90 backdrop-blur-md border-b border-outline/20",
    ) {
        attribute(ARIA_LABEL, "Main navigation")
        div(className = "container mx-auto px-4 h-16 flex items-center gap-4") {
            // Logo
            div(
                id = "nav-logo",
                className = "font-extrabold text-xl text-primary tracking-tight cursor-pointer " +
                    "flex-shrink-0 select-none",
            ) {
                role("link")
                tabindex(0)
                attribute(ARIA_LABEL, i18n.tr("KodEx home"))
                +"KodEx"
            }

            // Desktop nav links
            div(className = "hidden md:flex items-center gap-1") {
                navLink(key = "problems", label = i18n.tr("Problems"))
                navLink(key = "contests", label = i18n.tr("Contests"))
                navLink(key = "leaderboard", label = i18n.tr("Leaderboard"))
                if (canCreateExam) {
                    navLink(
                        key = "create-exam",
                        label = i18n.tr("Create Exam"),
                        elementId = "nav-create-exam",
                    )
                }
            }

            div(className = "flex-1") {}

            // Toggles
            ThemeSwitcher(id = "nav-theme-toggle", className = "hidden md:flex")
            LanguageSwitcher(id = "nav-lang-toggle", className = "hidden md:flex")

            // Auth section
            if (session is SessionState.Guest) {
                guestButtons()
            } else if (session is SessionState.Authenticated) {
                authenticatedSection(session = session, dropdownOpen = dropdownOpen) {
                    dropdownOpen = !dropdownOpen
                }
            }

            // Hamburger — mobile only
            div(
                id = "nav-hamburger",
                className = "md:hidden w-9 h-9 flex flex-col items-center justify-center gap-1.5 " +
                    "rounded-lg hover:bg-surface-variant cursor-pointer " +
                    FOCUS_VISIBLE_OUTLINE,
            ) {
                tabindex(0)
                role("button")
                attribute(ARIA_LABEL, i18n.tr("Open menu"))
                attribute("aria-expanded", drawerOpen.toString())
                span(className = W_5_H_0_5_BG_ON_SURFACE_ROUNDED_FULL_TRANSITION_ALL) {}
                span(className = W_5_H_0_5_BG_ON_SURFACE_ROUNDED_FULL_TRANSITION_ALL) {}
                span(className = W_5_H_0_5_BG_ON_SURFACE_ROUNDED_FULL_TRANSITION_ALL) {}
                onClick { drawerOpen = !drawerOpen }
                onKeydown { e -> if (e.key == ENTER || e.key == " ") drawerOpen = !drawerOpen }
            }
        }

        // Mobile drawer
        if (drawerOpen) {
            mobileDrawer(
                session = session,
                canCreateExam = canCreateExam,
                onClose = { drawerOpen = false },
            )
        }
    }
}

@Composable
private fun IComponent.navLink(key: String, label: String, elementId: String = "nav-link-$key") {
    div(
        id = elementId,
        className = "relative px-3 py-1.5 rounded-lg text-sm cursor-pointer text-on-surface/70 " +
            "hover:text-on-surface hover:bg-surface-variant transition-colors duration-150 " +
            FOCUS_VISIBLE_OUTLINE,
    ) {
        tabindex(0)
        role("link")
        span { +label }
        // Underline indicator
        div(
            className = "absolute bottom-0 start-0 end-0 h-0.5 bg-primary scale-x-0 " +
                "group-hover:scale-x-100 origin-start transition-transform duration-200 rounded-full",
        ) {}
    }
}

@Composable
private fun IComponent.guestButtons() {
    div(
        id = "nav-sign-in",
        className = "hidden md:flex px-3 py-1.5 text-sm cursor-pointer text-on-surface/70 " +
            "hover:text-on-surface transition-colors duration-150 rounded-lg " +
            "hover:bg-surface-variant " +
            FOCUS_VISIBLE_OUTLINE,
    ) {
        tabindex(0)
        role("link")
        attribute(ARIA_LABEL, i18n.tr("Sign in to your account"))
        +i18n.tr("Sign In")
    }
    Button(
        id = "nav-sign-up",
        label = i18n.tr("Sign Up"),
        variant = ButtonVariant.Primary,
        className = "hidden md:inline-flex",
    )
}

@Composable
private fun IComponent.authenticatedSection(
    session: SessionState.Authenticated,
    dropdownOpen: Boolean,
    onToggle: () -> Unit,
) {
    div(className = "flex items-center gap-2") {
        // Plan badge
        div(
            id = "nav-plan-badge",
            className = "hidden md:flex items-center px-2 py-0.5 rounded-full text-xs font-medium " +
                if (session.plan == Plan.PRO) {
                    "bg-secondary/15 text-secondary"
                } else {
                    "bg-surface-variant text-on-surface/60"
                },
        ) {
            attribute("aria-live", "polite")
            +if (session.plan == Plan.PRO) "Pro" else "Free"
        }

        // Username
        span(
            id = "nav-username",
            className = "hidden md:block text-sm text-on-surface/80 font-medium",
        ) {
            +session.username
        }

        // Avatar
        div(
            id = "nav-avatar",
            className = "w-8 h-8 rounded-full flex items-center justify-center cursor-pointer " +
                "bg-primary text-white text-xs font-bold select-none " +
                FOCUS_VISIBLE_OUTLINE,
        ) {
            tabindex(0)
            role("button")
            attribute(ARIA_LABEL, i18n.tr("Profile menu"))
            attribute("aria-haspopup", "menu")
            attribute("aria-expanded", dropdownOpen.toString())
            +session.username.take(1).uppercase()
            onClick { onToggle() }
            onKeydown { e ->
                if (e.key == ENTER || e.key == " ") onToggle()
                if (e.key == "Escape" && dropdownOpen) onToggle()
            }
        }
    }

    if (dropdownOpen) {
        profileDropdown(username = session.username) { onToggle() }
    }
}

@Composable
private fun IComponent.profileDropdown(username: String, onClose: () -> Unit) {
    div(
        id = "nav-profile-dropdown",
        className = "absolute end-4 top-16 z-50 min-w-52 rounded-xl bg-surface border " +
            "border-outline/20 shadow-xl py-1 overflow-hidden",
    ) {
        role("menu")
        attribute(ARIA_LABEL, i18n.tr("Profile menu for $username"))

        val menuItems = listOf(
            "nav-menu-profile" to i18n.tr("My Profile"),
            "nav-menu-badges" to i18n.tr("My Badges"),
            "nav-menu-trophies" to i18n.tr("My Trophies"),
            "nav-menu-stats" to i18n.tr("My Stats"),
            "nav-menu-contests" to i18n.tr("My Contests"),
            "nav-menu-settings" to i18n.tr("Settings"),
        )
        menuItems.forEach { (id, label) ->
            dropdownItem(id = id, label = label, onClose = onClose)
        }
        div(className = "h-px bg-outline/10 my-1") {}
        dropdownItem(
            id = "nav-menu-logout",
            label = i18n.tr("Log Out"),
            className = "text-error hover:bg-error/10",
            onClose = onClose,
        )
    }

    // Backdrop to close on outside click
    div(className = "fixed inset-0 z-40") {
        onClick {
            onClose()
            // Animate dropdown close
            document.getElementById("nav-profile-dropdown")?.let { el ->
                gsap.to(
                    el,
                    unsafeJso {
                        opacity = 0.0
                        duration = DROPDOWN_DURATION
                    },
                )
            }
        }
    }
}

@Composable
private fun IComponent.dropdownItem(
    id: String,
    label: String,
    className: String = "text-on-surface hover:bg-surface-variant",
    onClose: () -> Unit,
) {
    div(
        id = id,
        className = "flex items-center px-4 py-2 text-sm cursor-pointer transition-colors duration-100 " +
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset " +
            "focus-visible:ring-primary/50 $className",
    ) {
        tabindex(0)
        role("menuitem")
        +label
        onClick { onClose() }
        onKeydown { e ->
            if (e.key == ENTER || e.key == " ") onClose()
            if (e.key == "Escape") onClose()
        }
    }
}

@Composable
private fun IComponent.mobileDrawer(session: SessionState, canCreateExam: Boolean, onClose: () -> Unit) {
    div(
        id = "nav-mobile-drawer",
        className = "md:hidden border-t border-outline/20 bg-surface px-4 py-4 flex flex-col gap-2",
    ) {
        role("dialog")
        attribute(ARIA_LABEL, i18n.tr("Mobile navigation menu"))

        listOf(
            "problems" to i18n.tr("Problems"),
            "contests" to i18n.tr("Contests"),
            "leaderboard" to i18n.tr("Leaderboard"),
        ).forEach { (key, label) ->
            div(
                id = "nav-mobile-$key",
                className = "px-3 py-2 rounded-lg text-sm text-on-surface/70 hover:text-on-surface " +
                    "hover:bg-surface-variant cursor-pointer transition-colors",
            ) {
                tabindex(0)
                +label
                onClick { onClose() }
            }
        }

        if (canCreateExam) {
            div(
                id = "nav-mobile-create-exam",
                className = "px-3 py-2 rounded-lg text-sm text-primary font-medium " +
                    "hover:bg-primary/10 cursor-pointer transition-colors",
            ) {
                tabindex(0)
                +i18n.tr("Create Exam")
                onClick { onClose() }
            }
        }

        div(className = "h-px bg-outline/10 my-1") {}

        div(className = "flex items-center gap-3 px-2") {
            ThemeSwitcher(id = "nav-mobile-theme-toggle")
            LanguageSwitcher(id = "nav-mobile-lang-toggle")
        }

        if (session is SessionState.Guest) {
            div(className = "flex flex-col gap-2 mt-2") {
                div(
                    id = "nav-mobile-sign-in",
                    className = "px-3 py-2 rounded-lg text-sm text-center text-on-surface/70 " +
                        "hover:bg-surface-variant cursor-pointer transition-colors border border-outline/20",
                ) {
                    +i18n.tr("Sign In")
                }
                div(
                    id = "nav-mobile-sign-up",
                    className = "px-3 py-2 rounded-lg text-sm text-center text-white bg-primary " +
                        "hover:bg-primary/90 cursor-pointer transition-colors",
                ) {
                    +i18n.tr("Sign Up")
                }
            }
        }
    }
}

private val CREATOR_ROLES = setOf(UserRole.EXAM_CREATOR, UserRole.ADMIN)
private const val DROPDOWN_DURATION = 0.18
private const val ARIA_LABEL = "aria-label"
private const val FOCUS_VISIBLE_OUTLINE =
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50"
private const val W_5_H_0_5_BG_ON_SURFACE_ROUNDED_FULL_TRANSITION_ALL =
    "w-5 h-0.5 bg-on-surface rounded-full transition-all"
private const val ENTER = "Enter"
