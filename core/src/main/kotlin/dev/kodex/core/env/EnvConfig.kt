package dev.kodex.core.env

import io.github.cdimascio.dotenv.dotenv

private val DOT_ENV = dotenv()

fun env(key: String): String = System.getenv(key)
    ?: DOT_ENV[key]
    ?: error("Missing required env var: $key")

fun envOrNull(key: String): String? = System.getenv(key) ?: DOT_ENV[key]
