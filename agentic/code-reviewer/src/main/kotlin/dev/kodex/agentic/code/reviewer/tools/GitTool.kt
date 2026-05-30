package dev.kodex.agentic.code.reviewer.tools

object GitTool {
    fun getDiff(): String {
        return runCmd("git diff origin/develop...HEAD")
    }

    private fun runCmd(cmd: String): String {
        val parts = cmd.split(" ")
        val process = ProcessBuilder(parts).start()
        return process.inputStream.bufferedReader().readText()
    }
}
