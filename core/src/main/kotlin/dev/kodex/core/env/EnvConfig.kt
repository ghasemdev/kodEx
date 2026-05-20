package dev.kodex.core.env

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()

fun env(key: String): String = System.getenv(key)
    ?: dotenv[key]
    ?: error("Missing required env var: $key")

fun envOrNull(key: String): String? = System.getenv(key) ?: dotenv[key]
