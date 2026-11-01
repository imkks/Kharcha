package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.component.AmountText
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.feature.transactions.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitRecordsScreen(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val records = remember(transactions) {
        transactions.filter { SplitExpenseCalculator.isSplitActive(it.note) }
            .sortedByDescending(Transaction::dateTime)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.split_records_title)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.financialColors.cardBackground),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.split_records_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.split_records_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                    }
                }
            } else {
                items(records, key = Transaction::id) { transaction ->
                    val merchant = transaction.merchantNormalised
                        ?: transaction.merchantRaw
                        ?: stringResource(R.string.split_records_unknown_merchant)
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onTransactionClick(transaction) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.financialColors.cardBackground),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = SplitExpenseCalculator.parseSplitSummary(transaction.note).orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                            AmountText(
                                amountMinor = transaction.amount,
                                type = transaction.type,
                                isCounted = transaction.isCounted,
                                isTransfer = transaction.isTransfer,
                            )
                        }
                    }
                }
            }
        }
    }
}
