package dev.kodex.webapp.design

import dev.kodex.webapp.design.breakpoint.BreakpointTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BreakpointTierTest {

    @Test
    fun hasFourTiers() {
        assertEquals(4, BreakpointTier.entries.size)
    }

    @Test
    fun containsAllExpectedValues() {
        val values = BreakpointTier.entries
        assertTrue(BreakpointTier.Mobile in values)
        assertTrue(BreakpointTier.Tablet in values)
        assertTrue(BreakpointTier.Desktop in values)
        assertTrue(BreakpointTier.Tv in values)
    }

    @Test
    fun ordinalOrdering_mobileToTv() {
        assertTrue(BreakpointTier.Mobile.ordinal < BreakpointTier.Tablet.ordinal)
        assertTrue(BreakpointTier.Tablet.ordinal < BreakpointTier.Desktop.ordinal)
        assertTrue(BreakpointTier.Desktop.ordinal < BreakpointTier.Tv.ordinal)
    }

    @Test
    fun valueOf_returnsCorrectTier() {
        assertEquals(BreakpointTier.Mobile, BreakpointTier.valueOf("Mobile"))
        assertEquals(BreakpointTier.Tv, BreakpointTier.valueOf("Tv"))
    }
}
