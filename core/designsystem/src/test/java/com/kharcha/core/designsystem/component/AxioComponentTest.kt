package com.kharcha.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AxioComponentTest {
    @Test
    fun testAxioNavItemData() {
        val painter = ColorPainter(Color.Green)
        val item = AxioNavItem(
            icon = painter,
            contentDescription = "Home",
        )
        assertNotNull(item.icon)
        assertEquals("Home", item.contentDescription)
    }
}
