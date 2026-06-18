package dev.kodex.server.domain.auth.usecase

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal val MAGIC_LINK_TTL = 1.hours
internal val OTP_TTL = 2.minutes
internal val REFRESH_TOKEN_TTL = 7.days
