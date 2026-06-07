package dev.kodex.server.api.util

import kotlin.uuid.Uuid

private val UUID_REGEX = Regex(
    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE,
)

fun sanitizeRequestId(raw: String?): String =
    if (raw != null && raw.matches(UUID_REGEX)) raw else Uuid.random().toString()
