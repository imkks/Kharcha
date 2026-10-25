package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R

data class AccountOption(
    val key: String,
    val name: String,
    val accountIds: Set<Long>,
    val channels: Set<Channel>,
    val totalExpenseMinor: Long,
    val transactionCount: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSheet(
    transactions: List<Transaction>,
    selectedAccountIds: Set<Long>,
    selectedChannels: Set<Channel>,
    onSelectAccount: (accountIds: Set<Long>, channels: Set<Channel>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isPrivacy = LocalPrivacyMode.current

    val totalExpense =
        remember(transactions) {
            transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
        }

    val accountOptions =
        remember(transactions) {
            val groups =
                transactions.groupBy { txn ->
                    val last4 = txn.refNumber?.takeLast(4)?.trim()
                    when {
                        !last4.isNullOrEmpty() -> "HDFC $last4"
                        txn.channel != null -> "${txn.channel.name} Account"
                        else -> "Account #${txn.accountId}"
                    }
                }
            groups.map { (name, txns) ->
                val accIds = txns.map { it.accountId }.toSet()
                val channels = txns.mapNotNull { it.channel }.toSet()
                val expenseSum =
                    txns.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                AccountOption(
                    key = name,
                    name = name,
                    accountIds = accIds,
                    channels = channels,
                    totalExpenseMinor = expenseSum,
                    transactionCount = txns.size,
                )
            }.sortedByDescending { it.totalExpenseMinor }
        }

    val isAllAccountsSelected = selectedAccountIds.isEmpty() && selectedChannels.isEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                text = stringResource(id = R.string.accounts_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.accounts_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                // All Accounts Row
                item {
                    val allExpenseFormatted =
                        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(totalExpense)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (isAllAccountsSelected) {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                            ),
                        border =
                            if (isAllAccountsSelected) {
                                BorderStroke(1.dp, MaterialTheme.financialColors.income.copy(alpha = 0.5f))
                            } else {
                                null
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAccount(emptySet(), emptySet())
                                    onDismiss()
                                },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_bank),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.income,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.accounts_all),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.accounts_txn_count,
                                            transactions.size,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = allExpenseFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (isAllAccountsSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.income,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                // Individual Accounts
                items(accountOptions, key = { it.key }) { account ->
                    val isSelected =
                        !isAllAccountsSelected &&
                            (selectedAccountIds.intersect(account.accountIds).isNotEmpty() ||
                                selectedChannels.intersect(account.channels).isNotEmpty())
                    val expenseFormatted =
                        if (isPrivacy) {
                            "₹ ••••••"
                        } else {
                            MoneyFormatter.formatMinorUnits(account.totalExpenseMinor)
                        }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                            ),
                        border =
                            if (isSelected) {
                                BorderStroke(1.dp, MaterialTheme.financialColors.income.copy(alpha = 0.5f))
                            } else {
                                null
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAccount(account.accountIds, account.channels)
                                    onDismiss()
                                },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(40.dp),
                            ) {
                                val iconRes =
                                    if (account.name.contains("CARD", ignoreCase = true)) {
                                        DesignR.drawable.ic_channel_card
                                    } else if (account.name.contains("UPI", ignoreCase = true)) {
                                        DesignR.drawable.ic_channel_upi
                                    } else {
                                        DesignR.drawable.ic_bank
                                    }
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.income,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.accounts_txn_count,
                                            account.transactionCount,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = expenseFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.income,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
