package com.kharcha.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KharchaTypographyTest {
    @Test
    fun testTabularNumbersConfiguredAcrossStyles() {
        assertEquals("tnum", KharchaTypography.displayLarge.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.displayMedium.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.headlineLarge.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.headlineMedium.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.titleLarge.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.titleMedium.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.titleSmall.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.bodyLarge.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.bodyMedium.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.bodySmall.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.labelLarge.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.labelMedium.fontFeatureSettings)
        assertEquals("tnum", KharchaTypography.labelSmall.fontFeatureSettings)
    }

    @Test
    fun testTypographyScaleHierarchy() {
        assertNotNull(KharchaTypography.displayLarge.fontSize)
        assertNotNull(KharchaTypography.headlineLarge.fontSize)
        assertNotNull(KharchaTypography.bodyLarge.fontSize)
    }
}
