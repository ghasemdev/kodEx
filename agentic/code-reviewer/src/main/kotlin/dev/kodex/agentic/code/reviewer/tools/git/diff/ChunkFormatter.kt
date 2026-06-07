package dev.kodex.agentic.code.reviewer.tools.git.diff

import dev.kodex.agentic.code.reviewer.tools.git.diff.models.DiffChunk

class ChunkFormatter {
    fun format(chunk: DiffChunk): String {
        return buildString {
            appendLine("# FILE CONTEXT")

            chunk.files.forEach { file ->
                appendLine("\n## File: ${file.filePath}")
                file.hunks.forEach { hunk ->
                    appendLine("\n${hunk.header}")
                    appendLine(hunk.content)
                }
            }
        }
    }
}
