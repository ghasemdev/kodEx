package dev.kodex.agentic.code.reviewer.tools.git.diff.models

data class DiffChunk(
    val files: List<FileDiff>,
    val estimatedSize: Int
)
