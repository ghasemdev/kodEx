package dev.kodex.core.env

fun env(key: String): String =
    System.getenv(key) ?: error("Missing required env var: $key")

fun envOrNull(key: String): String? = System.getenv(key)
