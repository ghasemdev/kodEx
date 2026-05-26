package dev.kodex.webapp.design

import dev.kodex.webapp.design.components.ToastLevel
import dev.kodex.webapp.design.components.ToastMessage
import dev.kodex.webapp.design.components.ToastStore
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ToastStoreTest : FunSpec({
    beforeEach { ToastStore.TOASTS.clear() }
    afterEach { ToastStore.TOASTS.clear() }

    context("ToastMessage defaults") {
        test("level defaults to Info") {
            ToastMessage(message = "hello").level shouldBe ToastLevel.Info
        }
        test("duration defaults to 3 500 ms") {
            ToastMessage(message = "hello").duration shouldBe 3500.milliseconds
        }
        test("message is stored verbatim") {
            ToastMessage(message = "hello world").message shouldBe "hello world"
        }
    }

    context("ToastStore.show") {
        test("adds one toast") {
            ToastStore.show("Test")
            ToastStore.TOASTS shouldHaveSize 1
            ToastStore.TOASTS[0].message shouldBe "Test"
        }

        test("respects custom level") {
            ToastStore.show("Boom", ToastLevel.Error)
            ToastStore.TOASTS[0].level shouldBe ToastLevel.Error
        }

        test("respects custom duration") {
            ToastStore.show("Slow", ToastLevel.Info, 10.seconds)
            ToastStore.TOASTS[0].duration shouldBe 10.seconds
        }

        test("appends multiple TOASTS in insertion order") {
            ToastStore.show("A")
            ToastStore.show("B")
            ToastStore.show("C")
            ToastStore.TOASTS shouldHaveSize 3
            ToastStore.TOASTS[0].message shouldBe "A"
            ToastStore.TOASTS[2].message shouldBe "C"
        }
    }

    context("ToastStore.dismiss") {
        test("removes the targeted toast and keeps others") {
            ToastStore.show("A")
            ToastStore.show("B")
            val a = ToastStore.TOASTS[0]
            ToastStore.dismiss(a)
            ToastStore.TOASTS shouldHaveSize 1
            ToastStore.TOASTS[0].message shouldBe "B"
        }

        test("is a no-op for an unknown toast") {
            ToastStore.show("A")
            ToastStore.dismiss(ToastMessage(message = "ghost"))
            ToastStore.TOASTS shouldHaveSize 1
        }
    }

    context("ToastLevel enum") {
        test("has four variants") { ToastLevel.entries shouldHaveSize 4 }
        test("contains all expected levels") {
            val levels = ToastLevel.entries
            levels shouldContain ToastLevel.Info
            levels shouldContain ToastLevel.Success
            levels shouldContain ToastLevel.Warning
            levels shouldContain ToastLevel.Error
        }
    }
})
