package com.kharcha.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FinancialThemeTokensTest {
    @Test
    fun testAxioColorTokensMatchSpecification() {
        assertEquals(Color(0xFF0E0B16), axio_background_dark)
        assertEquals(Color(0xFF171326), axio_background_secondary)
        assertEquals(Color(0xFF1C1730), axio_surface_dark)
        assertEquals(Color(0xFF26203B), axio_surface_high)
        assertEquals(Color(0xFF302947), axio_surface_highest)
        assertEquals(Color(0xFF8B5CF6), axio_lime_accent)
        assertEquals(Color(0xFFC4B5FD), axio_pale_lime)
        assertEquals(Color(0xFF1C1730), axio_on_pale_lime)
        assertEquals(Color(0xFF22D3EE), axio_chart_blue)
        assertEquals(Color(0xFF1C1730), axio_account_navy)
        assertEquals(Color(0xFF122B35), axio_online_tag_bg)
        assertEquals(Color(0xFF22D3EE), axio_online_tag_text)
        assertEquals(Color(0xFF352D4A), axio_hairline_divider)
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
        assertEquals(Color(0xFF22D3EE), colors.income)
        assertEquals(Color(0xFF8B5CF6), colors.limeAccent)
        assertEquals(Color(0xFF22D3EE), colors.chartBlue)
    }

    @Test
    fun testLightFinancialColorsFallback() {
        val colors = LightFinancialColors
        assertNotNull(colors.limeAccent)
        assertNotNull(colors.cardBackground)
    }
}
