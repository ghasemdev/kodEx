package dev.kodex.webapp.design.components

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ComponentEnumsTest : FunSpec({
    context("ButtonVariant") {
        test("has five values") { ButtonVariant.entries shouldHaveSize 5 }
        test("contains all expected values") {
            val v = ButtonVariant.entries
            v shouldContain ButtonVariant.Primary
            v shouldContain ButtonVariant.Secondary
            v shouldContain ButtonVariant.Ghost
            v shouldContain ButtonVariant.Danger
            v shouldContain ButtonVariant.Link
        }
    }

    context("BadgeVariant") {
        test("has six values") { BadgeVariant.entries shouldHaveSize 6 }
        test("contains all expected values") {
            val v = BadgeVariant.entries
            v shouldContain BadgeVariant.Default
            v shouldContain BadgeVariant.Primary
            v shouldContain BadgeVariant.Success
            v shouldContain BadgeVariant.Warning
            v shouldContain BadgeVariant.Danger
            v shouldContain BadgeVariant.Info
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
