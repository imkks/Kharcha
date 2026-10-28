package com.kharcha.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kharcha.core.designsystem.R
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.categorySource ?: "Other") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(id = R.string.txn_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CategoryBadge(
                            categoryName = selectedCategory,
                            size = 48.dp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val merchantName =
                            transaction.merchantNormalised
                                ?: transaction.merchantRaw
                                ?: stringResource(id = R.string.unknown_merchant)

                        Text(
                            text = merchantName,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AmountText(
                            amountMinor = transaction.amount,
                            type = selectedType,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val dateStr = formatDateTime(transaction.dateTime)
                        val channelStr = transaction.channel?.name ?: "SMS"
                        Text(
                            text = stringResource(id = R.string.txn_detail_channel, "$channelStr • $dateStr"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        transaction.refNumber?.let { ref ->
                            Text(
                                text = stringResource(id = R.string.txn_detail_ref, ref),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        transaction.upiId?.let { upi ->
                            Text(
                                text = stringResource(id = R.string.txn_detail_upi, upi),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        val noteText = transaction.note
                        if (!noteText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.txn_detail_message),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = noteText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple(
                            TransactionType.EXPENSE,
                            stringResource(id = R.string.add_txn_type_expense),
                            R.drawable.ic_trending_down,
                        ),
                        Triple(
                            TransactionType.INCOME,
                            stringResource(id = R.string.add_txn_type_income),
                            R.drawable.ic_trending_up,
                        ),
                    ).forEach { (type, label, iconRes) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                            },
                            label = { Text(text = label) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.txn_detail_category),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DEFAULT_CATEGORIES.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = category
                            },
                            label = { Text(text = category) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(
                                        id = CategoryIconHelper.getCategoryIcon(category),
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.txn_detail_close))
                    }
                    Spacer(modifier = Modifier.padding(end = 8.dp))
                    Button(
                        onClick = {
                            val updated =
                                transaction.copy(
                                    type = selectedType,
                                    categorySource = selectedCategory,
                                    isReviewed = true,
                                )
                            onSave(updated)
                            onDismiss()
                        },
                    ) {
                        Text(text = stringResource(id = R.string.txn_detail_save))
                    }
                }
            }
        }
    }
}
