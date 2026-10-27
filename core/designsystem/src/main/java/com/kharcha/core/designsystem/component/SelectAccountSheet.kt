package com.kharcha.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType

/**
 * Bottom sheet to select a source account (CASH, Bank Accounts, Cards, Wallets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccountSheet(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Account?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    includeAllAccountsOption: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.financialColors.cardBackground,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.select_account_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (includeAllAccountsOption) {
                    item {
                        AccountSelectionRow(
                            title = "All accounts",
                            subtitle = "Aggregate across all active accounts",
                            iconRes = R.drawable.ic_bank,
                            isSelected = selectedAccountId == null,
                            onClick = {
                                onAccountSelected(null)
                                onDismiss()
                            },
                        )
                    }
                }

                items(accounts, key = { it.id }) { acc ->
                    val iconRes =
                        when (acc.type) {
                            AccountType.CreditCard -> R.drawable.ic_channel_card
                            AccountType.Wallet -> R.drawable.ic_channel_wallet
                            else -> R.drawable.ic_bank
                        }
                    val subtitle =
                        if (acc.last4 != null) {
                            "•••• ${acc.last4}"
                        } else {
                            acc.type.name
                        }

                    AccountSelectionRow(
                        title = acc.name,
                        subtitle = subtitle,
                        iconRes = iconRes,
                        isSelected = selectedAccountId == acc.id,
                        onClick = {
                            onAccountSelected(acc)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSelectionRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hairline = MaterialTheme.financialColors.hairlineDivider
    val lime = MaterialTheme.financialColors.income
    val border = if (isSelected) BorderStroke(1.5.dp, lime) else BorderStroke(1.dp, hairline)

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = border,
        color = if (isSelected) Color(0xFF1E2B20) else Color(0xFF262626),
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1B2228),
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = lime,
                        modifier = Modifier.padding(9.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = lime,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
