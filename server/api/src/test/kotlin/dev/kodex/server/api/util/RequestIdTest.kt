package dev.kodex.server.api.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

private val UUID_PATTERN =
    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

class RequestIdTest : FunSpec({
    context("valid UUID") {
        test("lowercase UUID is passed through unchanged") {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            sanitizeRequestId(uuid) shouldBe uuid
        }

        test("uppercase UUID is passed through unchanged") {
            val uuid = "550E8400-E29B-41D4-A716-446655440000"
            sanitizeRequestId(uuid) shouldBe uuid
        }
    }

    context("invalid input replaced with fresh UUID") {
        test("null returns a valid UUID") {
            sanitizeRequestId(null) shouldMatch UUID_PATTERN
        }

        test("script injection is replaced") {
            val result = sanitizeRequestId("<script>alert(1)</script>")
            result shouldNotBe "<script>alert(1)</script>"
            result shouldMatch UUID_PATTERN
        }

        test("oversized string is replaced") {
            val result = sanitizeRequestId("a".repeat(500))
            result shouldMatch UUID_PATTERN
        }

        test("empty string is replaced") {
            sanitizeRequestId("") shouldMatch UUID_PATTERN
        }

        test("plain text is replaced") {
            sanitizeRequestId("not-a-uuid") shouldMatch UUID_PATTERN
        }
    }
})
