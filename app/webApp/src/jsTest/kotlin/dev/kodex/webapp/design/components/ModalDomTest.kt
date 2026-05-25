package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.renderComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLHeadingElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ModalDomTest {

    @Test
    fun modal_notVisible_rendersNothing() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = false, onDismiss = {}) { +"Content" }
        }
        val dialog = host.querySelector("[role='dialog']") as? HTMLElement
        assertNull(dialog, "modal should not render when visible=false")
        cleanupHost(host)
    }

    @Test
    fun modal_visible_rendersDialogRole() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = true, onDismiss = {}) { +"Content" }
        }
        val dialog = host.querySelector("[role='dialog']") as? HTMLElement
        assertNotNull(dialog, "modal should render [role=dialog] when visible=true")
        cleanupHost(host)
    }

    @Test
    fun modal_visible_hasAriaModalTrue() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = true, onDismiss = {}) { +"Hello" }
        }
        val dialog = host.querySelector("[role='dialog']") as? HTMLElement
        assertNotNull(dialog)
        assertEquals(dialog.getAttribute("aria-modal"), "true", "dialog should have aria-modal=true")
        cleanupHost(host)
    }

    @Test
    fun modal_withTitle_rendersH2WithId() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = true, title = "Confirm Delete", onDismiss = {}) { +"Body" }
        }
        val h2 = host.querySelector("h2#modal-title") as? HTMLHeadingElement
        assertNotNull(h2, "titled modal should render h2#modal-title")
        assertEquals(h2.textContent?.contains("Confirm Delete"), true)
        cleanupHost(host)
    }

    @Test
    fun modal_withTitle_hasAriaLabelledBy() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = true, title = "Info", onDismiss = {}) { +"Body" }
        }
        val dialog = host.querySelector("[role='dialog']") as? HTMLElement
        assertNotNull(dialog)
        assertEquals(
            dialog.getAttribute("aria-labelledby"),
            "modal-title",
            "titled modal should have aria-labelledby=modal-title"
        )
        cleanupHost(host)
    }

    @Test
    fun modal_withoutTitle_noH2() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = true, onDismiss = {}) { +"No title here" }
        }
        val h2 = host.querySelector("h2") as? HTMLHeadingElement
        assertNull(h2, "untitled modal should not render an h2")
        cleanupHost(host)
    }

    @Test
    fun modal_bodyContent_isRendered() = MainScope().promise {
        val host = renderComponent {
            Modal(visible = true, onDismiss = {}) { +"Slot body text" }
        }
        assertEquals(host.textContent?.contains("Slot body text"), true, "modal body content should be rendered")
        cleanupHost(host)
    }
}
