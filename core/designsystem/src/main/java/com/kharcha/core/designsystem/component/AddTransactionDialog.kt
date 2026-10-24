package com.kharcha.core.designsystem.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(TransactionType.Expense) }
    var amountInput by remember { mutableStateOf("") }
    var merchantInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedChannel by remember { mutableStateOf(Channel.UPI) }

    val amountMinor = MoneyFormatter.parseMinorUnits(amountInput)
    val isSaveEnabled = amountMinor != null && amountMinor > 0L

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(id = R.string.add_txn_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        TransactionType.Expense to stringResource(id = R.string.add_txn_type_expense),
                        TransactionType.Income to stringResource(id = R.string.add_txn_type_income),
                        TransactionType.Transfer to stringResource(id = R.string.add_txn_type_transfer),
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == TransactionType.Income && selectedCategory == "Food & Dining") {
                                    selectedCategory = "Salary"
                                }
                            },
                            label = { Text(text = label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            amountInput = filtered
                        }
                    },
                    leadingIcon = {
                        Text(
                            text = stringResource(id = R.string.add_txn_currency_symbol),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.add_txn_amount_hint),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = it },
                    label = { Text(text = stringResource(id = R.string.add_txn_merchant_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.add_txn_merchant_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.add_txn_channel_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Channel.UPI to "UPI",
                        Channel.CARD to "Card",
                        Channel.ATM to "Cash",
                        Channel.NEFT to "NetBanking",
                    ).forEach { (channel, label) ->
                        FilterChip(
                            selected = selectedChannel == channel,
                            onClick = { selectedChannel = channel },
                            label = { Text(text = label) },
                        )
                    }
                }

                if (selectedType != TransactionType.Transfer) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.txn_detail_category),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DEFAULT_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(text = category) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.add_txn_cancel))
                    }
                    Spacer(modifier = Modifier.padding(end = 8.dp))
                    Button(
                        onClick = {
                            if (amountMinor != null && amountMinor > 0L) {
                                val cleanName = merchantInput.trim().ifEmpty { selectedCategory }
                                val newTxn =
                                    Transaction(
                                        id = System.currentTimeMillis(),
                                        accountId = 1L,
                                        type = selectedType,
                                        amount = amountMinor,
                                        currency = "INR",
                                        dateTime = System.currentTimeMillis(),
                                        merchantRaw = cleanName,
                                        merchantNormalised = cleanName,
                                        categorySource = selectedCategory,
                                        channel = selectedChannel,
                                        isReviewed = true,
                                    )
                                onSave(newTxn)
                                onDismiss()
                            }
                        },
                        enabled = isSaveEnabled,
                    ) {
                        Text(text = stringResource(id = R.string.add_txn_save))
                    }
                }
            }
        }
    }
}
