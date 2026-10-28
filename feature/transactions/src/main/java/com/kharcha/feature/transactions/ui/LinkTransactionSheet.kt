package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionClassifierHelper
import com.kharcha.core.designsystem.component.AmountText
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkTransactionSheet(
    targetTransaction: Transaction,
    allTransactions: List<Transaction>,
    onLink: (candidate: Transaction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isTransferLink: Boolean = false,
    isCreditTarget: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val isLinkingToRefund = targetTransaction.type == TransactionType.EXPENSE

    val targetMerchant =
        targetTransaction.merchantNormalised
            ?: targetTransaction.merchantRaw
            ?: ""

    val eligibleCandidates =
        remember(allTransactions, targetTransaction, isTransferLink, isCreditTarget, isLinkingToRefund) {
            allTransactions.filter { candidate ->
                if (candidate.id == targetTransaction.id) return@filter false
                if (isTransferLink) {
                    if (isCreditTarget) {
                        // On credit side -> link debit
                        candidate.type == TransactionType.EXPENSE
                    } else {
                        // On debit side -> link credit
                        candidate.type == TransactionType.INCOME
                    }
                } else {
                    if (isLinkingToRefund) {
                        candidate.categorySource.equals("Refund", ignoreCase = true) ||
                            TransactionClassifierHelper.isRefundNarrative(candidate.note)
                    } else {
                        candidate.type == TransactionType.EXPENSE
                    }
                }
            }
        }

    val filteredList =
        remember(eligibleCandidates, searchQuery) {
            if (searchQuery.isBlank()) {
                eligibleCandidates
            } else {
                val q = searchQuery.trim().lowercase()
                eligibleCandidates.filter {
                    val mNorm = it.merchantNormalised?.lowercase() ?: ""
                    val mRaw = it.merchantRaw?.lowercase() ?: ""
                    val note = it.note?.lowercase() ?: ""
                    val amountStr = (it.amount / 100).toString()
                    mNorm.contains(q) || mRaw.contains(q) || note.contains(q) || amountStr.contains(q)
                }
            }
        }

    val suggested =
        remember(filteredList, targetMerchant, targetTransaction.amount) {
            filteredList.filter {
                val matchesMerchant =
                    targetMerchant.isNotBlank() &&
                        ((it.merchantNormalised?.contains(targetMerchant, ignoreCase = true) == true) ||
                            (it.merchantRaw?.contains(targetMerchant, ignoreCase = true) == true))
                val matchesAmount = it.amount == targetTransaction.amount
                matchesMerchant || matchesAmount
            }.take(4)
        }

    val others =
        remember(filteredList, suggested) {
            val suggestedIds = suggested.map { it.id }.toSet()
            filteredList.filter { !suggestedIds.contains(it.id) }
        }

    val title =
        if (isTransferLink) {
            if (isCreditTarget) {
                stringResource(id = R.string.link_sheet_debit_title)
            } else {
                stringResource(id = R.string.link_sheet_credit_title)
            }
        } else if (isLinkingToRefund) {
            stringResource(id = R.string.link_sheet_expense_title)
        } else {
            stringResource(id = R.string.link_sheet_refund_title)
        }

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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val targetAmountStr = MoneyFormatter.formatMinorUnits(targetTransaction.amount)
            val merchantLabel =
                if (targetMerchant.isNotBlank()) "• $targetMerchant" else ""
            Text(
                text = "$targetAmountStr $merchantLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.link_sheet_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                ) {
                    Text(
                        text = stringResource(id = R.string.link_sheet_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (suggested.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.link_sheet_suggested_header),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.income,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                            )
                        }
                        items(suggested, key = { "sug_${it.id}" }) { candidate ->
                            CandidateRow(
                                candidate = candidate,
                                isSuggested = true,
                                onLink = { onLink(candidate) },
                            )
                        }
                    }

                    if (others.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.link_sheet_all_header),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(others, key = { "other_${it.id}" }) { candidate ->
                            CandidateRow(
                                candidate = candidate,
                                isSuggested = false,
                                onLink = { onLink(candidate) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: Transaction,
    isSuggested: Boolean,
    onLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val merchant =
        candidate.merchantNormalised
            ?: candidate.merchantRaw
            ?: stringResource(id = R.string.detail_original_sms)

    val dateFormatted =
        remember(candidate.dateTime) {
            val date = Instant.ofEpochMilli(candidate.dateTime).atZone(ZoneId.systemDefault())
            date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
        }

    val channelText = candidate.channel?.name ?: "Bank"

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onLink() },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSuggested) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(categoryName = candidate.categorySource ?: merchant, size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateFormatted • $channelText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amountMinor = candidate.amount,
                    type = candidate.type,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onLink,
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.income,
                            contentColor = Color.Black,
                        ),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.height(28.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.link_sheet_action),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
