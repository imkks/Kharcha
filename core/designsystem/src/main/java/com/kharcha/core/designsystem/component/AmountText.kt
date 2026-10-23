package com.kharcha.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.expense_dark
import com.kharcha.core.designsystem.theme.expense_light
import com.kharcha.core.designsystem.theme.income_dark
import com.kharcha.core.designsystem.theme.income_light
import com.kharcha.core.designsystem.theme.transfer_dark
import com.kharcha.core.designsystem.theme.transfer_light
import com.kharcha.core.model.TransactionType

@Composable
fun AmountText(
    amountMinor: Long,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
) {
    val isIncome = type == TransactionType.Income
    val isTransfer = type == TransactionType.Transfer

    val formatted =
        when (type) {
            TransactionType.Income -> MoneyFormatter.formatMinorUnits(amountMinor, showSign = true, isCredit = true)
            TransactionType.Expense -> MoneyFormatter.formatMinorUnits(amountMinor, showSign = true, isCredit = false)
            else -> MoneyFormatter.formatMinorUnits(amountMinor, showSign = false)
        }

    val color =
        when {
            isTransfer -> if (useDarkTheme) transfer_dark else transfer_light
            isIncome -> if (useDarkTheme) income_dark else income_light
            else -> if (useDarkTheme) expense_dark else expense_light
        }

    Text(
        text = formatted,
        color = color,
        style = style,
        modifier = modifier,
    )
}
