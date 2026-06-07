package dev.kodex.webapp.landing

import dev.kodex.webapp.pages.landing.model.BadgeDefinition
import dev.kodex.webapp.pages.landing.model.Badges
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

class BadgeDefinitionTest : FunSpec({
    context("BadgeDefinition init-block validation") {
        test("all 8 badge ids match regex ^[a-z0-9-]+$") {
            Badges.all.forEach { badge ->
                badge.id shouldMatch Regex("^[a-z0-9-]+$")
            }
        }

        test("all 8 badge iconPaths match regex ^icons/badges/[a-z0-9/.-]+\\.png$") {
            Badges.all.forEach { badge ->
                badge.iconPath shouldMatch Regex("^icons/badges/[a-z0-9/.-]+\\.png$")
            }
        }

        test("no duplicate ids") {
            val ids = Badges.all.map { it.id }
            ids.distinct().size shouldBe ids.size
        }

        test("all fields are non-blank") {
            Badges.all.forEach { badge ->
                badge.id.isNotBlank() shouldBe true
                badge.name.isNotBlank() shouldBe true
                badge.description.isNotBlank() shouldBe true
                badge.unlockCondition.isNotBlank() shouldBe true
                badge.iconPath.isNotBlank() shouldBe true
            }
        }

        test("exactly 8 badges defined") {
            Badges.all.size shouldBe 8
        }

        test("init block rejects invalid id") {
            var threw = false
            try {
                BadgeDefinition(
                    id = "INVALID ID!",
                    name = "Test",
                    description = "Test",
                    unlockCondition = "Test",
                    iconPath = "icons/badges/test.png",
                )
            } catch (_: IllegalArgumentException) {
                threw = true
            }
            threw shouldBe true
        }

        test("init block rejects invalid iconPath") {
            var threw = false
            try {
                BadgeDefinition(
                    id = "test-badge",
                    name = "Test",
                    description = "Test",
                    unlockCondition = "Test",
                    iconPath = "wrong/path/icon.png",
                )
            } catch (_: IllegalArgumentException) {
                threw = true
            }
            threw shouldBe true
        }

        test("init block rejects blank name") {
            var threw = false
            try {
                BadgeDefinition(
                    id = "test-badge",
                    name = "   ",
                    description = "Test",
                    unlockCondition = "Test",
                    iconPath = "icons/badges/test.png",
                )
            } catch (_: IllegalArgumentException) {
                threw = true
            }
            threw shouldBe true
        }
    }
})
