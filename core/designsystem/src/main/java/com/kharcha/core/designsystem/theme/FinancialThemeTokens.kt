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
    val limeAccent: Color = axio_lime_accent,
    val onLimeAccent: Color = axio_on_pale_lime,
    val paleLimeAddButton: Color = axio_pale_lime,
    val onPaleLimeAddButton: Color = axio_on_pale_lime,
    val chartBlue: Color = axio_chart_blue,
    val chartBlueHighlight: Color = axio_chart_blue_highlight,
    val accountNavy: Color = axio_account_navy,
    val onlineTagBackground: Color = axio_online_tag_bg,
    val onlineTagText: Color = axio_online_tag_text,
    val cardBackground: Color = axio_surface_dark,
    val raisedRowBackground: Color = axio_surface_highest,
    val hairlineDivider: Color = axio_hairline_divider,
    val textPrimary: Color = axio_text_primary,
    val textSecondary: Color = axio_text_secondary,
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
        limeAccent = axio_lime_accent,
        paleLimeAddButton = axio_pale_lime,
        onPaleLimeAddButton = axio_on_pale_lime,
        chartBlue = axio_chart_blue,
        chartBlueHighlight = axio_chart_blue_highlight,
        accountNavy = axio_account_navy,
        onlineTagBackground = axio_online_tag_bg,
        onlineTagText = axio_online_tag_text,
        cardBackground = axio_surface_dark,
        raisedRowBackground = axio_surface_highest,
        hairlineDivider = axio_hairline_divider,
        textPrimary = axio_text_primary,
        textSecondary = axio_text_secondary,
    )

val DarkFinancialColors =
    FinancialColors(
        income = income_dark,
        onIncome = Color(0xFF121212),
        incomeContainer = Color(0xFF1B3811),
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
        heatmapLevel1 = Color(0xFF1B3811),
        heatmapLevel2 = Color(0xFF2E6319),
        heatmapLevel3 = Color(0xFF5CA32B),
        heatmapLevel4 = axio_lime_accent,
        limeAccent = axio_lime_accent,
        paleLimeAddButton = axio_pale_lime,
        onPaleLimeAddButton = axio_on_pale_lime,
        chartBlue = axio_chart_blue,
        chartBlueHighlight = axio_chart_blue_highlight,
        accountNavy = axio_account_navy,
        onlineTagBackground = axio_online_tag_bg,
        onlineTagText = axio_online_tag_text,
        cardBackground = axio_surface_dark,
        raisedRowBackground = axio_surface_highest,
        hairlineDivider = axio_hairline_divider,
        textPrimary = axio_text_primary,
        textSecondary = axio_text_secondary,
    )

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

val MaterialTheme.financialColors: FinancialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinancialColors.current
