package dev.kodex.webapp.design.components

import dev.kodex.webapp.design.cleanupHost
import dev.kodex.webapp.design.renderComponent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLSpanElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BadgeDomTest {

    @Test
    fun badge_rendersSpanElement() = MainScope().promise {
        val host = renderComponent { Badge { +"Beta" } }
        val span = host.querySelector("span") as? HTMLSpanElement
        assertNotNull(span, "expected <span> element")
        cleanupHost(host)
    }

    @Test
    fun badge_textContent_matchesSlot() = MainScope().promise {
        val host = renderComponent { Badge { +"New" } }
        val span = host.querySelector("span") as? HTMLSpanElement
        assertNotNull(span)
        assertEquals(span.textContent?.contains("New"), true, "badge text should match slot content")
        cleanupHost(host)
    }

    @Test
    fun badge_hasRoundedFullClass() = MainScope().promise {
        val host = renderComponent { Badge { +"X" } }
        val span = host.querySelector("span") as? HTMLSpanElement
        assertNotNull(span)
        assertTrue(
            span.className.contains("rounded-full"),
            "badge should have rounded-full class"
        )
        cleanupHost(host)
    }

    @Test
    fun badge_customClassName_isAppended() = MainScope().promise {
        val host = renderComponent { Badge(className = "extra-class") { +"X" } }
        val span = host.querySelector("span") as? HTMLSpanElement
        assertNotNull(span)
        assertTrue(span.className.contains("extra-class"))
        cleanupHost(host)
    }

    @Test
    fun badge_primary_hasTextPrimaryClass() = MainScope().promise {
        val host = renderComponent { Badge(variant = BadgeVariant.Primary) { +"P" } }
        val span = host.querySelector("span") as? HTMLSpanElement
        assertNotNull(span)
        assertTrue(
            span.className.contains("text-primary"),
            "primary badge should have text-primary class"
        )
        cleanupHost(host)
    }

    @Test
    fun badge_success_hasTextSuccessClass() = MainScope().promise {
        val host = renderComponent { Badge(variant = BadgeVariant.Success) { +"OK" } }
        val span = host.querySelector("span") as? HTMLSpanElement
        assertNotNull(span)
        assertTrue(span.className.contains("text-success"))
        cleanupHost(host)
    }
}
