package dev.kodex.webapp.design.components

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ComponentEnumsTest : FunSpec({
    context("ButtonVariant") {
        test("has five values") { ButtonVariant.entries shouldHaveSize 5 }
        test("contains all expected values") {
            val variants = ButtonVariant.entries
            variants shouldContain ButtonVariant.Primary
            variants shouldContain ButtonVariant.Secondary
            variants shouldContain ButtonVariant.Ghost
            variants shouldContain ButtonVariant.Danger
            variants shouldContain ButtonVariant.Link
        }
    }

    context("BadgeVariant") {
        test("has six values") { BadgeVariant.entries shouldHaveSize 6 }
        test("contains all expected values") {
            val variants = BadgeVariant.entries
            variants shouldContain BadgeVariant.Default
            variants shouldContain BadgeVariant.Primary
            variants shouldContain BadgeVariant.Success
            variants shouldContain BadgeVariant.Warning
            variants shouldContain BadgeVariant.Danger
            variants shouldContain BadgeVariant.Info
        }
    }

    context("ComponentSize") {
        test("has three values") { ComponentSize.entries shouldHaveSize 3 }
        test("ordinals Sm < Md < Lg") {
            (ComponentSize.Sm.ordinal < ComponentSize.Md.ordinal) shouldBe true
            (ComponentSize.Md.ordinal < ComponentSize.Lg.ordinal) shouldBe true
        }
    }

    context("NavItem") {
        test("stores key, label and icon") {
            val item = NavItem(key = "home", label = "Home", icon = "fa-house")
            item.key shouldBe "home"
            item.label shouldBe "Home"
            item.icon shouldBe "fa-house"
        }
        test("icon is optional and defaults to null") {
            NavItem(key = "home", label = "Home").icon shouldBe null
        }
        test("data class equality") {
            NavItem("home", "Home") shouldBe NavItem("home", "Home")
        }
    }
})
