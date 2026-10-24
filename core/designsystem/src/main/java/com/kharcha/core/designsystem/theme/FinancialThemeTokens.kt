package com.kharcha.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinancialColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val onTransfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val heatmapLevel0: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
)

val LightFinancialColors =
    FinancialColors(
        income = income_light,
        onIncome = Color.White,
        incomeContainer = Color(0xFFDCFCE7),
        expense = expense_light,
        onExpense = Color.White,
        expenseContainer = Color(0xFFFFEDD5),
        transfer = transfer_light,
        onTransfer = Color.White,
        transferContainer = Color(0xFFF1F5F9),
        warning = Color(0xFFB45309),
        onWarning = Color.White,
        warningContainer = Color(0xFFFEF3C7),
        heatmapLevel0 = Color(0xFFE2E8F0),
        heatmapLevel1 = Color(0xFF99F6E4),
        heatmapLevel2 = Color(0xFF2DD4BF),
        heatmapLevel3 = Color(0xFF0F766E),
        heatmapLevel4 = Color(0xFF115E59),
    )

val DarkFinancialColors =
    FinancialColors(
        income = income_dark,
        onIncome = Color(0xFF052E16),
        incomeContainer = Color(0xFF14532D),
        expense = expense_dark,
        onExpense = Color(0xFF431407),
        expenseContainer = Color(0xFF7C2D12),
        transfer = transfer_dark,
        onTransfer = Color(0xFF0F172A),
        transferContainer = Color(0xFF1E293B),
        warning = Color(0xFFFBBF24),
        onWarning = Color(0xFF451A03),
        warningContainer = Color(0xFF78350F),
        heatmapLevel0 = Color(0xFF334155),
        heatmapLevel1 = Color(0xFF115E59),
        heatmapLevel2 = Color(0xFF0D9488),
        heatmapLevel3 = Color(0xFF2DD4BF),
        heatmapLevel4 = Color(0xFF5EEAD4),
    )

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

val MaterialTheme.financialColors: FinancialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinancialColors.current
