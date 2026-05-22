package dev.kodex.core.logging

import org.slf4j.MDC

object LoggingConfig {
    fun setupMDC(requestId: String) {
        MDC.put("requestId", requestId)
    }
}
