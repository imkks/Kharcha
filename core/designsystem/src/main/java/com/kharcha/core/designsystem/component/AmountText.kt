package com.kharcha.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.TransactionType

@Composable
fun AmountText(
    amountMinor: Long,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maskIfPrivacy: Boolean = true,
    isExcluded: Boolean = false,
) {
    val isPrivacyMode = LocalPrivacyMode.current
    val formatted =
        if (maskIfPrivacy && isPrivacyMode) {
            "₹ ••••••"
        } else {
            when (type) {
                TransactionType.INCOME ->
                    MoneyFormatter.formatMinorUnits(amountMinor, showSign = true, isCredit = true)
                TransactionType.EXPENSE ->
                    MoneyFormatter.formatMinorUnits(amountMinor, showSign = true, isCredit = false)
            }
        }

    val financialColors = MaterialTheme.financialColors
    val color =
        if (isExcluded) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        } else {
            when (type) {
                TransactionType.INCOME -> financialColors.income
                TransactionType.EXPENSE -> financialColors.expense
            }
        }

    val textDecoration =
        if (isExcluded) TextDecoration.LineThrough else TextDecoration.None

    Text(
        text = formatted,
        color = color,
        style =
            style.copy(
                fontFeatureSettings = "tnum",
                textDecoration = textDecoration,
            ),
        modifier = modifier,
    )
}
