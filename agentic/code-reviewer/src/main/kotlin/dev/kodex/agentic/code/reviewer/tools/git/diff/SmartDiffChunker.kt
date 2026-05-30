package dev.kodex.agentic.code.reviewer.tools.git.diff

import dev.kodex.agentic.code.reviewer.tools.git.diff.models.DiffChunk
import dev.kodex.agentic.code.reviewer.tools.git.diff.models.DiffHunk
import dev.kodex.agentic.code.reviewer.tools.git.diff.models.FileDiff

class SmartDiffChunker(
    private val maxChunkSize: Int = 8000
) {
    fun chunk(files: List<FileDiff>): List<DiffChunk> {
        val chunks = mutableListOf<DiffChunk>()

        var currentFiles = mutableListOf<FileDiff>()
        var currentSize = 0

        fun flush() {
            if (currentFiles.isNotEmpty()) {
                chunks.add(
                    DiffChunk(
                        files = currentFiles.toList(),
                        estimatedSize = currentSize
                    )
                )
            }
            currentFiles = mutableListOf()
            currentSize = 0
        }

        files.forEach { file ->

            val fileSize = estimate(file)

            if (fileSize > maxChunkSize) {

                flush()

                chunks.addAll(splitLargeFile(file))
                return@forEach
            }

            if (currentSize + fileSize > maxChunkSize) {
                flush()
            }

            currentFiles.add(file)
            currentSize += fileSize
        }

        flush()

        return chunks
    }

    private fun splitLargeFile(file: FileDiff): List<DiffChunk> {
        val result = mutableListOf<DiffChunk>()

        var currentHunks = mutableListOf<DiffHunk>()
        var size = 0

        fun flush() {
            if (currentHunks.isNotEmpty()) {
                result.add(
                    DiffChunk(
                        files = listOf(
                            file.copy(hunks = currentHunks.toList())
                        ),
                        estimatedSize = size
                    )
                )
            }
            currentHunks = mutableListOf()
            size = 0
        }

        file.hunks.forEach { hunk ->

            val hunkSize = hunk.content.length

            if (size + hunkSize > maxChunkSize) {
                flush()
            }

            currentHunks.add(hunk)
            size += hunkSize
        }

        flush()

        return result
    }

    private fun estimate(file: FileDiff): Int {
        return file.hunks.sumOf { it.content.length } + file.filePath.length
    }
}
