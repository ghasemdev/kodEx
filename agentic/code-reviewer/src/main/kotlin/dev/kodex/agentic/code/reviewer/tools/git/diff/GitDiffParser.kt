package dev.kodex.agentic.code.reviewer.tools.git.diff

import dev.kodex.agentic.code.reviewer.tools.git.diff.models.DiffHunk
import dev.kodex.agentic.code.reviewer.tools.git.diff.models.FileDiff

class GitDiffParser {
    fun parse(diff: String): List<FileDiff> {
        val files = mutableListOf<FileDiff>()

        var currentFile: String? = null
        var currentHunks = mutableListOf<DiffHunk>()

        var currentHunkHeader = ""
        var currentHunkBody = StringBuilder()

        fun flushHunk() {
            if (currentHunkHeader.isNotEmpty()) {
                currentHunks.add(
                    DiffHunk(
                        header = currentHunkHeader,
                        content = currentHunkBody.toString(),
                    ),
                )
            }
            currentHunkHeader = ""
            currentHunkBody = StringBuilder()
        }

        fun flushFile() {
            if (currentFile != null) {
                flushHunk()
                files.add(FileDiff(currentFile!!, currentHunks.toList()))
            }
            currentHunks = mutableListOf()
        }

        diff.lines().forEach { line ->

            when {
                line.startsWith("diff --git") -> {
                    flushFile()
                    currentFile = extractFile(line)
                }

                line.startsWith("@@") -> {
                    flushHunk()
                    currentHunkHeader = line
                }

                else -> {
                    currentHunkBody.appendLine(line)
                }
            }
        }

        flushFile()

        return files
    }

    private fun extractFile(line: String): String = line.split(" ").lastOrNull()?.removePrefix("b/") ?: "unknown"
}
