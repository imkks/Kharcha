package com.kharcha.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryColorsTest {
    @Test
    fun testPaletteHas16Tokens() {
        assertEquals(16, CategoryColors.palette.size)
    }

    @Test
    fun testTokensAreDistinctAndValid() {
        val distinctColors = CategoryColors.palette.map { it.color }.toSet()
        assertEquals(16, distinctColors.size)

        CategoryColors.palette.forEach { token ->
            assertNotNull(token.color)
            assertNotNull(token.containerColor)
            assertNotNull(token.onContainerColor)
        }
    }

    @Test
    fun testForCategoryDeterministicMapping() {
        val token1 = CategoryColors.forCategory("Food & Dining")
        val token2 = CategoryColors.forCategory("Food & Dining")
        val token3 = CategoryColors.forCategory("food & dining")

        assertEquals(token1, token2)
        assertEquals(token1, token3)
    }

    @Test
    fun testForCategoryNullAndBlankHandling() {
        val tokenNull = CategoryColors.forCategory(null)
        val tokenEmpty = CategoryColors.forCategory("")
        val tokenWhitespace = CategoryColors.forCategory("   ")

        assertNotNull(tokenNull)
        assertNotNull(tokenEmpty)
        assertNotNull(tokenWhitespace)
        assertTrue(CategoryColors.palette.contains(tokenNull))
    }
}
