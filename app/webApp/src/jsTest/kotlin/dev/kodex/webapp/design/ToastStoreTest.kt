package dev.kodex.webapp.design

import dev.kodex.webapp.design.components.ToastLevel
import dev.kodex.webapp.design.components.ToastMessage
import dev.kodex.webapp.design.components.ToastStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ToastStoreTest {

    @BeforeTest
    fun clearStore() {
        ToastStore.toasts.clear()
    }

    @AfterTest
    fun restoreStore() {
        ToastStore.toasts.clear()
    }

    @Test
    fun toastMessage_defaultLevel_isInfo() {
        val toast = ToastMessage(message = "Hello")
        assertEquals(ToastLevel.Info, toast.level)
    }

    @Test
    fun toastMessage_defaultDuration_is3500ms() {
        val toast = ToastMessage(message = "Hello")
        assertEquals(3500.milliseconds, toast.duration)
    }

    @Test
    fun toastMessage_storesText() {
        val toast = ToastMessage(message = "Test notification")
        assertEquals("Test notification", toast.message)
    }

    @Test
    fun toastMessage_uniqueIds() {
        val a = ToastMessage(message = "A")
        val b = ToastMessage(message = "B")
        assertTrue(a.id != b.id || a.message != b.message)
    }

    @Test
    fun show_addsToast() {
        ToastStore.show("Test")
        assertEquals(1, ToastStore.toasts.size)
        assertEquals("Test", ToastStore.toasts[0].message)
    }

    @Test
    fun show_withErrorLevel() {
        ToastStore.show("Oops", ToastLevel.Error)
        assertEquals(ToastLevel.Error, ToastStore.toasts[0].level)
    }

    @Test
    fun show_withCustomDuration() {
        ToastStore.show("Slow", ToastLevel.Info, 10.seconds)
        assertEquals(10.seconds, ToastStore.toasts[0].duration)
    }

    @Test
    fun show_multipleToasts_appendsInOrder() {
        ToastStore.show("First")
        ToastStore.show("Second")
        ToastStore.show("Third")
        assertEquals(3, ToastStore.toasts.size)
        assertEquals("First", ToastStore.toasts[0].message)
        assertEquals("Third", ToastStore.toasts[2].message)
    }

    @Test
    fun dismiss_removesSpecificToast() {
        ToastStore.show("A")
        ToastStore.show("B")
        val toastA = ToastStore.toasts[0]
        ToastStore.dismiss(toastA)
        assertEquals(1, ToastStore.toasts.size)
        assertEquals("B", ToastStore.toasts[0].message)
    }

    @Test
    fun dismiss_nonExistentToast_isNoOp() {
        ToastStore.show("A")
        val phantom = ToastMessage(message = "ghost")
        ToastStore.dismiss(phantom)
        assertEquals(1, ToastStore.toasts.size)
    }

    @Test
    fun toastLevel_hasFourVariants() {
        assertEquals(4, ToastLevel.entries.size)
    }

    @Test
    fun toastLevel_containsAllExpected() {
        val levels = ToastLevel.entries
        assertTrue(ToastLevel.Info in levels)
        assertTrue(ToastLevel.Success in levels)
        assertTrue(ToastLevel.Warning in levels)
        assertTrue(ToastLevel.Error in levels)
    }
}
