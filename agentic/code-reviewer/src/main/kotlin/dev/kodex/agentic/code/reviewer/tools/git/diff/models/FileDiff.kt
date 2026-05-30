package dev.kodex.agentic.code.reviewer.tools.git.diff.models

data class FileDiff(
    val filePath: String,
    val hunks: List<DiffHunk>
)
