package com.kharcha.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors

enum class TransactionTypeSwitchOption {
    SPEND,
    INCOME,
}

/**
 * TypeSwitch component: Two contiguous pills for Spend vs Income.
 * Active pill is filled lime (#A8E04A) with dark (#121212) text;
 * Inactive pill is dark outlined with grey text.
 */
@Composable
fun TypeSwitch(
    selectedOption: TransactionTypeSwitchOption,
    onOptionSelected: (TransactionTypeSwitchOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hairline = MaterialTheme.financialColors.hairlineDivider
    val lime = MaterialTheme.financialColors.income
    val darkText = MaterialTheme.financialColors.onPaleLimeAddButton

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Spend Pill
        val isSpend = selectedOption == TransactionTypeSwitchOption.SPEND
        Surface(
            shape = RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp),
            border = BorderStroke(1.dp, if (isSpend) lime else hairline),
            color = if (isSpend) lime else MaterialTheme.financialColors.cardBackground,
            modifier =
                Modifier
                    .weight(1f)
                    .clickable { onOptionSelected(TransactionTypeSwitchOption.SPEND) },
        ) {
            Text(
                text = stringResource(id = R.string.type_switch_spend),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                fontWeight = if (isSpend) FontWeight.Bold else FontWeight.Medium,
                color = if (isSpend) darkText else Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }

        // Income Pill
        val isIncome = selectedOption == TransactionTypeSwitchOption.INCOME
        Surface(
            shape = RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp),
            border = BorderStroke(1.dp, if (isIncome) lime else hairline),
            color = if (isIncome) lime else MaterialTheme.financialColors.cardBackground,
            modifier =
                Modifier
                    .weight(1f)
                    .clickable { onOptionSelected(TransactionTypeSwitchOption.INCOME) },
        ) {
            Text(
                text = stringResource(id = R.string.type_switch_income),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                color = if (isIncome) darkText else Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}
