package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.DateTimeFormatterHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.SearchField
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.designsystem.theme.LocalIncomeHidden
import com.kharcha.core.designsystem.theme.LocalIncomeRevealed
import com.kharcha.core.designsystem.theme.LocalIncomeReveal
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.isCounted
import com.kharcha.feature.transactions.R

enum class SearchTab {
    TRANSACTIONS,
    SMS,
}

data class SmsSnippet(
    val id: Long,
    val sender: String,
    val dateEpoch: Long,
    val body: String,
    val isParsed: Boolean,
    val transaction: Transaction?,
)

/**
 * Universal Search Screen (UI Spec Section 7.5).
 * Opened from the search icon on Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    onBack: () -> Unit,
    onFilterClick: () -> Unit = {},
    onAddPrefilledTransaction: (SmsSnippet) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(SearchTab.TRANSACTIONS) }

    val filteredTransactions =
        remember(transactions, searchQuery) {
            if (searchQuery.isBlank()) {
                transactions
            } else {
                val q = searchQuery.trim().lowercase()
                transactions.filter { tx ->
                    (tx.merchantNormalised?.lowercase()?.contains(q) == true) ||
                        (tx.merchantRaw?.lowercase()?.contains(q) == true) ||
                        (tx.note?.lowercase()?.contains(q) == true) ||
                        (tx.categorySource?.lowercase()?.contains(q) == true) ||
                        (tx.refNumber?.lowercase()?.contains(q) == true) ||
                        (tx.upiId?.lowercase()?.contains(q) == true)
                }
            }
        }

    val smsSnippets =
        remember(transactions, searchQuery) {
            val allSms =
                transactions.map { tx ->
                    val store = tx.merchantRaw ?: "Store"
                    val ref = tx.refNumber ?: "N/A"
                    SmsSnippet(
                        id = tx.id,
                        sender = tx.channel?.name ?: "BANK-ALERT",
                        dateEpoch = tx.dateTime,
                        body = tx.note ?: "Txn of ₹${tx.amount / 100} at $store ref $ref",
                        isParsed = tx.isReviewed,
                        transaction = tx,
                    )
                }
            if (searchQuery.isBlank()) {
                allSms
            } else {
                val q = searchQuery.trim().lowercase()
                allSms.filter { it.body.lowercase().contains(q) || it.sender.lowercase().contains(q) }
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
            ) {
                // Top Search Bar
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.navigate_up),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Tabs with count badges & Filter icon
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.financialColors.limeAccent,
                        divider = {},
                        modifier = Modifier.weight(1f),
                    ) {
                        Tab(
                            selected = selectedTab == SearchTab.TRANSACTIONS,
                            onClick = { selectedTab = SearchTab.TRANSACTIONS },
                            text = {
                                val isSelected = selectedTab == SearchTab.TRANSACTIONS
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.search_tab_transactions,
                                            filteredTransactions.size,
                                        ),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.financialColors.limeAccent
                                        } else {
                                            MaterialTheme.financialColors.textSecondary
                                        },
                                )
                            },
                        )
                        Tab(
                            selected = selectedTab == SearchTab.SMS,
                            onClick = { selectedTab = SearchTab.SMS },
                            text = {
                                val isSelected = selectedTab == SearchTab.SMS
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.search_tab_sms,
                                            smsSnippets.size,
                                        ),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.financialColors.limeAccent
                                        } else {
                                            MaterialTheme.financialColors.textSecondary
                                        },
                                )
                            },
                        )
                    }

                    IconButton(onClick = onFilterClick) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_category_other),
                            contentDescription = "Filter",
                            tint = MaterialTheme.financialColors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            SearchTab.TRANSACTIONS -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (filteredTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.transactions_empty_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                            }
                        }
                    } else {
                        items(filteredTransactions, key = { it.id }) { txn ->
                            SearchTransactionRow(
                                transaction = txn,
                                onClick = { onTransactionClick(txn) },
                            )
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.financialColors.hairlineDivider,
                            )
                        }
                    }
                }
            }
            SearchTab.SMS -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (smsSnippets.isEmpty()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No matching SMS found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                            }
                        }
                    } else {
                        items(smsSnippets, key = { it.id }) { sms ->
                            SmsSearchCard(
                                sms = sms,
                                highlightQuery = searchQuery,
                                onClick = {
                                    if (sms.isParsed && sms.transaction != null) {
                                        onTransactionClick(sms.transaction)
                                    } else {
                                        onAddPrefilledTransaction(sms)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPrivacy = LocalPrivacyMode.current
    val incomeHidden = LocalIncomeHidden.current?.value == true && LocalIncomeRevealed.current?.value != true
    val revealIncome = LocalIncomeReveal.current
    val isIncomeMasked = incomeHidden && transaction.type == TransactionType.Income
    val isTransfer = transaction.isTransfer
    val isDebit = transaction.type == TransactionType.Expense
    val arrowGlyph = when {
        isTransfer -> "⇄"
        isDebit -> "↗"
        else -> "↙"
    }
    val merchant = transaction.merchantNormalised ?: transaction.merchantRaw ?: "Unknown Merchant"
    val category = transaction.categorySource ?: "Others"
    val relativeDate = DateTimeFormatterHelper.formatRelativeTime(transaction.dateTime)
    val isCounted = transaction.isCounted

    val rowModifier =
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (!isCounted) {
                    Modifier.alpha(0.68f)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 12.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(categoryName = category, size = 36.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.financialColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.financialColors.textSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$arrowGlyph ${if (isPrivacy || isIncomeMasked) "₹ ••••••" else MoneyFormatter.formatMinorUnits(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    isTransfer -> MaterialTheme.financialColors.transfer
                    isDebit -> MaterialTheme.financialColors.textPrimary
                    else -> MaterialTheme.financialColors.limeAccent
                },
                modifier = if (isIncomeMasked && !isPrivacy) Modifier.clickable { revealIncome?.invoke() } else Modifier,
            )
            Text(
                text = relativeDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.financialColors.textSecondary,
            )
        }
    }
}

@Composable
fun SmsSearchCard(
    sms: SmsSnippet,
    highlightQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatted = DateTimeFormatterHelper.formatDetailDateTime(sms.dateEpoch)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.financialColors.cardBackground,
            ),
        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Sender & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sms.sender,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.financialColors.limeAccent,
                )
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.financialColors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body with lime bold highlight for search match
            val annotatedBody =
                remember(sms.body, highlightQuery) {
                    buildAnnotatedString {
                        val body = sms.body
                        if (highlightQuery.isBlank()) {
                            append(body)
                        } else {
                            val lowerBody = body.lowercase()
                            val lowerQuery = highlightQuery.trim().lowercase()
                            var startIndex = 0
                            while (startIndex < body.length) {
                                val matchIndex = lowerBody.indexOf(lowerQuery, startIndex)
                                if (matchIndex == -1) {
                                    append(body.substring(startIndex))
                                    break
                                }
                                append(body.substring(startIndex, matchIndex))
                                withStyle(
                                    style =
                                        SpanStyle(
                                            color = Color(0xFFA8E04A),
                                            fontWeight = FontWeight.Bold,
                                        ),
                                ) {
                                    append(body.substring(matchIndex, matchIndex + lowerQuery.length))
                                }
                                startIndex = matchIndex + lowerQuery.length
                            }
                        }
                    }
                }

            Text(
                text = annotatedBody,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.financialColors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Parsed / Not parsed chip & chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color =
                        if (sms.isParsed) {
                            MaterialTheme.financialColors.limeAccent.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.financialColors.raisedRowBackground
                        },
                ) {
                    Text(
                        text = if (sms.isParsed) "Parsed" else "Not parsed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            if (sms.isParsed) {
                                MaterialTheme.financialColors.limeAccent
                            } else {
                                MaterialTheme.financialColors.textSecondary
                            },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }

                Icon(
                    painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.financialColors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
