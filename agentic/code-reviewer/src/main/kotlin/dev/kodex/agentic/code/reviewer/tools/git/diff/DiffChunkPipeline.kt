package dev.kodex.agentic.code.reviewer.tools.git.diff

import kotlin.collections.map

class DiffChunkPipeline {
    private val parser = GitDiffParser()
    private val chunker = SmartDiffChunker()
    private val formatter = ChunkFormatter()

    fun process(diff: String): List<String> {

        val files = parser.parse(diff)
        val chunks = chunker.chunk(files)

        return chunks.map { chunk ->
            formatter.format(chunk)
        }
    }
}
