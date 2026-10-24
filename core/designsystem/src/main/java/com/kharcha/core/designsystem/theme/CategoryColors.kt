package com.kharcha.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class CategoryColorToken(
    val color: Color,
    val containerColor: Color,
    val onContainerColor: Color,
)

/**
 * Fixed palette of 16 distinguishable, color-blind friendly colors
 * assigned deterministically per category name, per SRS Section 6 & UI Spec Section 2.1.
 */
object CategoryColors {
    val palette: List<CategoryColorToken> =
        listOf(
            CategoryColorToken(
                color = Color(0xFFE53935), // Red - Dining / Eating out
                containerColor = Color(0xFFFFEBEE),
                onContainerColor = Color(0xFFB71C1C),
            ),
            CategoryColorToken(
                color = Color(0xFF1E88E5), // Blue - Transport / Transit
                containerColor = Color(0xFFE3F2FD),
                onContainerColor = Color(0xFF0D47A1),
            ),
            CategoryColorToken(
                color = Color(0xFF43A047), // Green - Groceries
                containerColor = Color(0xFFE8F5E9),
                onContainerColor = Color(0xFF1B5E20),
            ),
            CategoryColorToken(
                color = Color(0xFFFB8C00), // Orange - Shopping
                containerColor = Color(0xFFFFF3E0),
                onContainerColor = Color(0xFFE65100),
            ),
            CategoryColorToken(
                color = Color(0xFF8E24AA), // Purple - Entertainment
                containerColor = Color(0xFFF3E5F5),
                onContainerColor = Color(0xFF4A148C),
            ),
            CategoryColorToken(
                color = Color(0xFF00ACC1), // Cyan - Bills & Utilities
                containerColor = Color(0xFFE0F7FA),
                onContainerColor = Color(0xFF006064),
            ),
            CategoryColorToken(
                color = Color(0xFF6D4C41), // Brown - Rent & Housing
                containerColor = Color(0xFFEFEBE9),
                onContainerColor = Color(0xFF3E2723),
            ),
            CategoryColorToken(
                color = Color(0xFFD81B60), // Pink - Personal Care
                containerColor = Color(0xFFFCE4EC),
                onContainerColor = Color(0xFF880E4F),
            ),
            CategoryColorToken(
                color = Color(0xFF3949AB), // Indigo - Subscriptions
                containerColor = Color(0xFFE8EAF6),
                onContainerColor = Color(0xFF1A237E),
            ),
            CategoryColorToken(
                color = Color(0xFF00897B), // Teal - Health & Medical
                containerColor = Color(0xFFE0F2F1),
                onContainerColor = Color(0xFF004D40),
            ),
            CategoryColorToken(
                color = Color(0xFFC0CA33), // Lime - Education
                containerColor = Color(0xFFF9FBE7),
                onContainerColor = Color(0xFF827717),
            ),
            CategoryColorToken(
                color = Color(0xFF546E7A), // Blue Grey - Investments
                containerColor = Color(0xFFECEFF1),
                onContainerColor = Color(0xFF263238),
            ),
            CategoryColorToken(
                color = Color(0xFFFF7043), // Deep Orange - Loans & EMI
                containerColor = Color(0xFFFBE9E7),
                onContainerColor = Color(0xFFBF360C),
            ),
            CategoryColorToken(
                color = Color(0xFF7CB342), // Light Green - Salary / Income
                containerColor = Color(0xFFF1F8E9),
                onContainerColor = Color(0xFF33691E),
            ),
            CategoryColorToken(
                color = Color(0xFFAB47BC), // Medium Purple - Gifts & Donations
                containerColor = Color(0xFFF3E5F5),
                onContainerColor = Color(0xFF6A1B9A),
            ),
            CategoryColorToken(
                color = Color(0xFF78909C), // Slate - Other / Miscellaneous
                containerColor = Color(0xFFECEFF1),
                onContainerColor = Color(0xFF37474F),
            ),
        )

    fun forCategory(categoryName: String?): CategoryColorToken {
        val clean = categoryName?.trim()?.lowercase() ?: return palette.last()
        val index = abs(clean.hashCode()) % palette.size
        return palette[index]
    }
}
