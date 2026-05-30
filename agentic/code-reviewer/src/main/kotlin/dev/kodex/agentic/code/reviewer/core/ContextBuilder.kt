package dev.kodex.agentic.code.reviewer.core

class ContextBuilder {
    fun enrich(chunk: String): String {
        return """
# CODE CONTEXT (CHUNK)

$chunk

# INSTRUCTIONS
Focus only on this scope.
Do not assume other files unless explicitly shown.
"""
    }
}
