package com.kharcha.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

@Composable
fun PeriodSelector(
    periodName: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
        }
        SuggestionChip(
            onClick = { /* Open period selector menu */ },
            label = { Text(periodName) },
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
    }
}

@Composable
fun SummaryCard(
    netAmount: Long,
    totalIncome: Long,
    totalExpense: Long,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Net Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatAmount(netAmount),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (netAmount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Income", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatAmount(totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Expense", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatAmount(totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF9800),
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTransactions(
    transactions: List<Transaction>,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
            androidx.compose.material3.TextButton(onClick = onSeeAll) {
                Text("See All")
            }
        }
        if (transactions.isEmpty()) {
            Text(
                "No data yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
            )
        } else {
            transactions.forEach { txn ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(txn.merchantName ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        formatAmount(txn.amountMinor),
                        color =
                            if (txn.type == TransactionType.INCOME) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                }
            }
        }
    }
}

fun formatAmount(amountMinor: Long): String {
    val amount = amountMinor / 100.0
    return "₹%.2f".format(amount)
}
