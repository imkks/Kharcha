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
    isCounted: Boolean = true,
    isTransfer: Boolean = false,
) {
    val isPrivacyMode = LocalPrivacyMode.current
    val formatted =
        if (maskIfPrivacy && isPrivacyMode) {
            "₹ ••••••"
        } else {
            when (type) {
                TransactionType.Income ->
                    MoneyFormatter.formatMinorUnits(amountMinor, showSign = true, isCredit = true)
                TransactionType.Expense ->
                    MoneyFormatter.formatMinorUnits(amountMinor, showSign = true, isCredit = false)
            }
        }

    val financialColors = MaterialTheme.financialColors
    val color =
        when {
            !isCounted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            isTransfer -> financialColors.transfer
            type == TransactionType.Income -> financialColors.income
            else -> financialColors.expense
        }

    val textDecoration =
        if (!isCounted) TextDecoration.LineThrough else TextDecoration.None

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
