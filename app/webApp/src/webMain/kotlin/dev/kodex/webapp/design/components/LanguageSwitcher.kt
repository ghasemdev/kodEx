package dev.kodex.webapp.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.span
import dev.kilua.i18n.LocaleManager
import dev.kodex.webapp.design.i18n.setLocale

private data class LangOption(val code: String, val nativeName: String, val flag: String)

private val languages = listOf(
    LangOption("en", "English", "🇬🇧"),
    LangOption("fa", "فارسی", "🇮🇷"),
)

@Composable
fun IComponent.LanguageSwitcher(className: String? = null) {
    val currentLang = LocaleManager.currentLocale.language.take(2)
    var open by remember { mutableStateOf(false) }
    val current = languages.find { it.code == currentLang } ?: languages[0]

    div(className = "relative ${className ?: ""}".trim()) {
        // Toggle button
        div(
            className = "cursor-pointer flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg " +
                "bg-surface-container hover:bg-surface-variant text-on-surface text-sm " +
                "transition-colors duration-150 select-none " +
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50"
        ) {
            tabindex(0)
            role("button")
            attribute("aria-haspopup", "listbox")
            attribute("aria-expanded", open.toString())
            span { +current.flag }
            span(className = "font-medium") { +current.nativeName }
            span(className = "text-on-surface/50 text-xs") { +"▾" }
            onClick { open = !open }
            onKeydown { e -> if (e.key == "Enter" || e.key == " ") open = !open }
        }

        // Dropdown
        if (open) {
            div(
                className = "absolute end-0 top-full mt-1 z-50 min-w-32 rounded-xl overflow-hidden " +
                    "bg-surface border border-outline/20 shadow-lg py-1"
            ) {
                role("listbox")
                languages.forEach { lang ->
                    val isSelected = lang.code == currentLang
                    div(
                        className = "flex items-center gap-2 px-3 py-2 cursor-pointer text-sm " +
                            "transition-colors duration-100 " +
                            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 " +
                            if (isSelected) {
                                "bg-primary/10 text-primary font-medium"
                            } else {
                                "text-on-surface hover:bg-surface-variant"
                            }
                    ) {
                        tabindex(0)
                        role("option")
                        attribute("aria-selected", isSelected.toString())
                        span { +lang.flag }
                        span { +lang.nativeName }
                        onClick {
                            setLocale(lang.code)
                            open = false
                        }
                        onKeydown { e ->
                            if (e.key == "Enter" || e.key == " ") {
                                setLocale(lang.code)
                                open = false
                            }
                            if (e.key == "Escape") open = false
                        }
                    }
                }
            }
        }
    }
}
