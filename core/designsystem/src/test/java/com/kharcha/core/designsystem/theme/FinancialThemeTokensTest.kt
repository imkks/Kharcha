package com.kharcha.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FinancialThemeTokensTest {
    @Test
    fun testAxioColorTokensMatchSpecification() {
        assertEquals(Color(0xFF121212), axio_background_dark)
        assertEquals(Color(0xFF161616), axio_background_secondary)
        assertEquals(Color(0xFF232323), axio_surface_dark)
        assertEquals(Color(0xFF2A2A2A), axio_surface_high)
        assertEquals(Color(0xFF2E2E2E), axio_surface_highest)
        assertEquals(Color(0xFFA8E04A), axio_lime_accent)
        assertEquals(Color(0xFFC8F08F), axio_pale_lime)
        assertEquals(Color(0xFF121212), axio_on_pale_lime)
        assertEquals(Color(0xFF4B4FE6), axio_chart_blue)
        assertEquals(Color(0xFF1F2D3D), axio_account_navy)
        assertEquals(Color(0xFF1E293B), axio_online_tag_bg)
        assertEquals(Color(0xFF38BDF8), axio_online_tag_text)
        assertEquals(Color(0xFF333333), axio_hairline_divider)
    }

    @Test
    fun testDarkFinancialColorsContainAxioTokens() {
        val colors = DarkFinancialColors
        assertNotNull(colors.limeAccent)
        assertNotNull(colors.paleLimeAddButton)
        assertNotNull(colors.onPaleLimeAddButton)
        assertNotNull(colors.chartBlue)
        assertNotNull(colors.accountNavy)
        assertNotNull(colors.onlineTagBackground)
        assertNotNull(colors.onlineTagText)
        assertNotNull(colors.cardBackground)
        assertNotNull(colors.raisedRowBackground)
        assertNotNull(colors.hairlineDivider)

        assertEquals(axio_lime_accent, colors.limeAccent)
        assertEquals(axio_pale_lime, colors.paleLimeAddButton)
        assertEquals(axio_chart_blue, colors.chartBlue)
        assertEquals(axio_account_navy, colors.accountNavy)
        assertEquals(axio_surface_dark, colors.cardBackground)
    }

    @Test
    fun testLightFinancialColorsFallback() {
        val colors = LightFinancialColors
        assertNotNull(colors.limeAccent)
        assertNotNull(colors.cardBackground)
    }
}
