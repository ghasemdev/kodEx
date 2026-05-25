package dev.kodex.webapp.design

import dev.kodex.webapp.design.breakpoint.BreakpointTier
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class BreakpointTierTest : FunSpec({
    test("has exactly four tiers") {
        BreakpointTier.entries.size shouldBe 4
    }

    test("contains all expected values") {
        val values = BreakpointTier.entries
        values shouldContain BreakpointTier.Mobile
        values shouldContain BreakpointTier.Tablet
        values shouldContain BreakpointTier.Desktop
        values shouldContain BreakpointTier.Tv
    }

    test("ordinals are in ascending screen-size order") {
        val (mobile, tablet, desktop, tv) = listOf(
            BreakpointTier.Mobile.ordinal,
            BreakpointTier.Tablet.ordinal,
            BreakpointTier.Desktop.ordinal,
            BreakpointTier.Tv.ordinal,
        )
        (mobile < tablet) shouldBe true
        (tablet < desktop) shouldBe true
        (desktop < tv) shouldBe true
    }

    test("valueOf returns correct tier") {
        BreakpointTier.valueOf("Mobile") shouldBe BreakpointTier.Mobile
        BreakpointTier.valueOf("Tv") shouldBe BreakpointTier.Tv
    }
})
