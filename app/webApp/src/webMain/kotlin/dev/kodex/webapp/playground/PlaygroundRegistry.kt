package dev.kodex.webapp.playground

import androidx.compose.runtime.Composable
import dev.kilua.core.IComponent

data class PlaygroundEntry(
    val name: String,
    val group: String,
    val content: @Composable IComponent.() -> Unit,
)

fun buildPlaygroundEntries(entries: List<PlaygroundEntry> = emptyList()): List<PlaygroundEntry> = entries
