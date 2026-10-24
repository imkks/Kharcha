package com.kharcha.feature.transactions.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionFilterMatcher
import com.kharcha.core.model.AmountRangeFilter
import com.kharcha.core.model.Channel
import com.kharcha.core.model.DateRangeSelection
import com.kharcha.core.model.ReviewStatusFilter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R

private val AVAILABLE_CATEGORIES =
    listOf(
        "Food & Dining",
        "Groceries",
        "Shopping",
        "Transport",
        "Bills & Utilities",
        "Entertainment",
        "Health",
        "Investments",
        "Salary",
        "Transfer",
        "Other",
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    transactions: List<Transaction>,
    initialFilter: TransactionFilter,
    onApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var draftFilter by remember { mutableStateOf(initialFilter) }

    val matchingTransactions =
        remember(transactions, draftFilter) {
            transactions.filter { TransactionFilterMatcher.matches(it, draftFilter) }
        }
    val matchingCount = matchingTransactions.size
    val matchingTotal =
        remember(matchingTransactions) {
            matchingTransactions.sumOf { it.amount }
        }
    val formattedTotal = MoneyFormatter.formatMinorUnits(matchingTotal)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            // Header Row: Title + Reset Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { draftFilter = TransactionFilter() }) {
                    Text(text = stringResource(id = R.string.filter_reset))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable filter options
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
            ) {
                // 1. Date Range
                SectionTitle(text = stringResource(id = R.string.filter_section_date))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val dateOptions =
                        listOf(
                            DateRangeSelection.AllTime to stringResource(id = R.string.filter_date_all),
                            DateRangeSelection.Today to stringResource(id = R.string.filter_date_today),
                            DateRangeSelection.ThisWeek to stringResource(id = R.string.filter_date_this_week),
                            DateRangeSelection.ThisMonth to stringResource(id = R.string.filter_date_this_month),
                            DateRangeSelection.LastMonth to stringResource(id = R.string.filter_date_last_month),
                            DateRangeSelection.LastNDays(30) to stringResource(id = R.string.filter_date_last_30_days),
                        )
                    dateOptions.forEach { (option, label) ->
                        FilterChip(
                            selected = draftFilter.dateRange == option,
                            onClick = { draftFilter = draftFilter.copy(dateRange = option) },
                            label = { Text(text = label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Transaction Type
                SectionTitle(text = stringResource(id = R.string.filter_section_type))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val types =
                        listOf(
                            TransactionType.Expense to stringResource(id = R.string.transactions_filter_expense),
                            TransactionType.Income to stringResource(id = R.string.transactions_filter_income),
                            TransactionType.Transfer to stringResource(id = R.string.transactions_filter_transfer),
                        )
                    types.forEach { (type, label) ->
                        val isSelected = draftFilter.types.contains(type)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newTypes =
                                    if (isSelected) {
                                        draftFilter.types - type
                                    } else {
                                        draftFilter.types + type
                                    }
                                draftFilter = draftFilter.copy(types = newTypes)
                            },
                            label = { Text(text = label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Category Multi-select
                SectionTitle(text = stringResource(id = R.string.filter_section_category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AVAILABLE_CATEGORIES.forEach { category ->
                        val isSelected = draftFilter.categories.contains(category)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newCategories =
                                    if (isSelected) {
                                        draftFilter.categories - category
                                    } else {
                                        draftFilter.categories + category
                                    }
                                draftFilter = draftFilter.copy(categories = newCategories)
                            },
                            label = { Text(text = category) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Amount Range
                SectionTitle(text = stringResource(id = R.string.filter_section_amount))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val amountOptions =
                        listOf(
                            AmountRangeFilter.Any to stringResource(id = R.string.filter_amount_any),
                            AmountRangeFilter.Under100 to stringResource(id = R.string.filter_amount_under_100),
                            AmountRangeFilter.Between100And500 to stringResource(id = R.string.filter_amount_100_500),
                            AmountRangeFilter.Between500And2000 to stringResource(id = R.string.filter_amount_500_2k),
                            AmountRangeFilter.Between2000And10000 to
                                stringResource(id = R.string.filter_amount_2k_10k),
                            AmountRangeFilter.Above10000 to stringResource(id = R.string.filter_amount_above_10k),
                        )
                    amountOptions.forEach { (option, label) ->
                        FilterChip(
                            selected = draftFilter.amountRange == option,
                            onClick = { draftFilter = draftFilter.copy(amountRange = option) },
                            label = { Text(text = label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Payment Channel
                SectionTitle(text = stringResource(id = R.string.filter_section_channel))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val channels =
                        listOf(
                            Channel.UPI to "UPI",
                            Channel.CARD to "Card",
                            Channel.NEFT to "NEFT / NetBanking",
                            Channel.ATM to "ATM",
                            Channel.WALLET to "Wallet",
                        )
                    channels.forEach { (channel, label) ->
                        val isSelected = draftFilter.channels.contains(channel)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newChannels =
                                    if (isSelected) {
                                        draftFilter.channels - channel
                                    } else {
                                        draftFilter.channels + channel
                                    }
                                draftFilter = draftFilter.copy(channels = newChannels)
                            },
                            label = { Text(text = label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Review Status
                SectionTitle(text = stringResource(id = R.string.filter_section_status))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val statusOptions =
                        listOf(
                            ReviewStatusFilter.All to stringResource(id = R.string.filter_status_all),
                            ReviewStatusFilter.NeedsReviewOnly to
                                stringResource(id = R.string.filter_status_needs_review),
                            ReviewStatusFilter.ReviewedOnly to stringResource(id = R.string.filter_status_reviewed),
                        )
                    statusOptions.forEach { (status, label) ->
                        FilterChip(
                            selected = draftFilter.reviewStatus == status,
                            onClick = { draftFilter = draftFilter.copy(reviewStatus = status) },
                            label = { Text(text = label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Sticky Bottom CTA with Live Count & Sum
            Button(
                onClick = {
                    onApply(draftFilter)
                    onDismiss()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            id = R.string.filter_action_apply,
                            matchingCount,
                            formattedTotal,
                        ),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
