package dev.kodex.core.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TierTest : FunSpec({
    test("JUNIOR displayName and colorToken") {
        Tier.JUNIOR.displayName shouldBe "Junior Coder"
        Tier.JUNIOR.colorToken shouldBe "tier-junior"
    }

    test("SENIOR displayName and colorToken") {
        Tier.SENIOR.displayName shouldBe "Senior Coder"
        Tier.SENIOR.colorToken shouldBe "tier-senior"
    }

    test("MASTER displayName and colorToken") {
        Tier.MASTER.displayName shouldBe "Master"
        Tier.MASTER.colorToken shouldBe "tier-master"
    }

    test("GRANDMASTER displayName and colorToken") {
        Tier.GRANDMASTER.displayName shouldBe "Grandmaster"
        Tier.GRANDMASTER.colorToken shouldBe "tier-grandmaster"
    }

    test("all four tiers exist") {
        Tier.entries.size shouldBe 4
    }
})
