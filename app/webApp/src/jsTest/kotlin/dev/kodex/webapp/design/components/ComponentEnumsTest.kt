package dev.kodex.webapp.design.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComponentEnumsTest {

    @Test
    fun buttonVariant_hasFiveValues() {
        assertEquals(5, ButtonVariant.entries.size)
    }

    @Test
    fun buttonVariant_containsAllExpected() {
        val values = ButtonVariant.entries
        assertTrue(ButtonVariant.Primary in values)
        assertTrue(ButtonVariant.Secondary in values)
        assertTrue(ButtonVariant.Ghost in values)
        assertTrue(ButtonVariant.Danger in values)
        assertTrue(ButtonVariant.Link in values)
    }

    @Test
    fun badgeVariant_hasSixValues() {
        assertEquals(6, BadgeVariant.entries.size)
    }

    @Test
    fun badgeVariant_containsAllExpected() {
        val values = BadgeVariant.entries
        assertTrue(BadgeVariant.Default in values)
        assertTrue(BadgeVariant.Primary in values)
        assertTrue(BadgeVariant.Success in values)
        assertTrue(BadgeVariant.Warning in values)
        assertTrue(BadgeVariant.Danger in values)
        assertTrue(BadgeVariant.Info in values)
    }

    @Test
    fun componentSize_hasThreeValues() {
        assertEquals(3, ComponentSize.entries.size)
    }

    @Test
    fun componentSize_containsAllExpected() {
        val values = ComponentSize.entries
        assertTrue(ComponentSize.Sm in values)
        assertTrue(ComponentSize.Md in values)
        assertTrue(ComponentSize.Lg in values)
    }

    @Test
    fun componentSize_ordinalOrdering() {
        assertTrue(ComponentSize.Sm.ordinal < ComponentSize.Md.ordinal)
        assertTrue(ComponentSize.Md.ordinal < ComponentSize.Lg.ordinal)
    }

    @Test
    fun navItem_storesKeyLabelAndIcon() {
        val item = NavItem(key = "home", label = "Home", icon = "fa-house")
        assertEquals("home", item.key)
        assertEquals("Home", item.label)
        assertEquals("fa-house", item.icon)
    }

    @Test
    fun navItem_iconIsOptional() {
        val item = NavItem(key = "home", label = "Home")
        assertEquals(null, item.icon)
    }

    @Test
    fun navItem_equality() {
        val a = NavItem("home", "Home")
        val b = NavItem("home", "Home")
        assertEquals(a, b)
    }
}
