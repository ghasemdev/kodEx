package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.renderComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLButtonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ButtonDomTest {

    @Test
    fun button_rendersButtonElement() = MainScope().promise {
        val host = renderComponent { Button(label = "Submit") }
        val btn = host.querySelector("button") as? HTMLButtonElement
        assertNotNull(btn, "expected <button> element")
        cleanupHost(host)
    }

    @Test
    fun button_labelAppearsInTextContent() = MainScope().promise {
        val host = renderComponent { Button(label = "Click me") }
        val btn = host.querySelector("button") as? HTMLButtonElement
        assertNotNull(btn)
        assertEquals(btn.textContent?.contains("Click me"), true, "button text should contain label")
        cleanupHost(host)
    }

    @Test
    fun button_disabled_whenNotEnabled() = MainScope().promise {
        val host = renderComponent { Button(label = "Save", enabled = false) }
        val btn = host.querySelector("button") as? HTMLButtonElement
        assertNotNull(btn)
        assertTrue(btn.disabled, "disabled button should have disabled property set")
        cleanupHost(host)
    }

    @Test
    fun button_notDisabled_whenEnabled() = MainScope().promise {
        val host = renderComponent { Button(label = "Save", enabled = true) }
        val btn = host.querySelector("button") as? HTMLButtonElement
        assertNotNull(btn)
        assertTrue(!btn.disabled, "enabled button should not be disabled")
        cleanupHost(host)
    }

    @Test
    fun button_customClassName_isOnElement() = MainScope().promise {
        val host = renderComponent { Button(label = "X", className = "my-custom") }
        val btn = host.querySelector("button") as? HTMLButtonElement
        assertNotNull(btn)
        assertTrue(
            btn.className.contains("my-custom"),
            "custom className should be present on button"
        )
        cleanupHost(host)
    }
}
