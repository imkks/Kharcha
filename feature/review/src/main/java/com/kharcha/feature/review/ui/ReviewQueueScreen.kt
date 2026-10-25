package com.kharcha.feature.review.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.DateTimeFormatterHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.DEFAULT_CATEGORIES
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.review.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQueueScreen(
    unreviewedTransactions: List<Transaction>,
    onCategorize: (Transaction, String, Boolean) -> Unit,
    onIgnore: (Transaction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var reviewedCount by rememberSaveable { mutableIntStateOf(0) }
    var rulesLearnedCount by rememberSaveable { mutableIntStateOf(0) }
    var showCategorySheet by remember { mutableStateOf(false) }

    val totalCount = unreviewedTransactions.size
    val isComplete = unreviewedTransactions.isEmpty() || currentIndex >= totalCount

    BackHandler(enabled = true) {
        onBack()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.review_screen_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!isComplete && totalCount > 0) {
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.review_step_indicator,
                                            currentIndex + 1,
                                            totalCount,
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.review_back_content_description),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
                if (!isComplete && totalCount > 0) {
                    val progress = (currentIndex + 1).toFloat() / totalCount.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (isComplete) {
                ReviewCompletionContent(
                    reviewedCount = reviewedCount,
                    rulesLearnedCount = rulesLearnedCount,
                    onDone = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val txn = unreviewedTransactions[currentIndex]
                AnimatedContent(
                    targetState = txn,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    },
                    label = "ReviewCardTransition",
                ) { currentTxn ->
                    ReviewCardContent(
                        transaction = currentTxn,
                        onAcceptCategory = { chosenCategory, propagate ->
                            onCategorize(currentTxn, chosenCategory, propagate)
                            reviewedCount++
                            if (propagate) rulesLearnedCount++
                            currentIndex++
                        },
                        onIgnore = {
                            onIgnore(currentTxn)
                            reviewedCount++
                            currentIndex++
                        },
                        onSkip = {
                            currentIndex++
                        },
                        onOpenCategoryPicker = { showCategorySheet = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (showCategorySheet) {
                    ReviewCategoryPickerSheet(
                        onCategorySelected = { chosenCategory ->
                            showCategorySheet = false
                            onCategorize(txn, chosenCategory, true)
                            reviewedCount++
                            rulesLearnedCount++
                            currentIndex++
                        },
                        onDismiss = { showCategorySheet = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCardContent(
    transaction: Transaction,
    onAcceptCategory: (category: String, propagate: Boolean) -> Unit,
    onIgnore: () -> Unit,
    onSkip: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val isPrivacy = LocalPrivacyMode.current
    val scrollState = rememberScrollState()

    val merchantDisplay =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: stringResource(id = com.kharcha.core.designsystem.R.string.unknown_merchant)

    val relativeTime = DateTimeFormatterHelper.formatRelativeTime(transaction.dateTime)
    val subtitle =
        buildString {
            append(relativeTime)
            transaction.channel?.let {
                append(" • ")
                append(it.name)
            }
        }

    var rememberRule by remember(transaction.id) {
        mutableStateOf(transaction.merchantNormalised != null || transaction.merchantRaw != null)
    }

    val suggestedCategories =
        remember(transaction.id) {
            extractSuggestedCategories(transaction)
        }

    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Hero Amount
                val amountColor =
                    when (transaction.type) {
                        TransactionType.Income, TransactionType.Refund -> MaterialTheme.financialColors.income
                        TransactionType.Expense -> MaterialTheme.financialColors.expense
                        TransactionType.Transfer -> MaterialTheme.financialColors.transfer
                        TransactionType.Investment -> MaterialTheme.colorScheme.primary
                        TransactionType.Ignored -> MaterialTheme.colorScheme.outline
                    }

                val amountFormatted =
                    if (isPrivacy) {
                        "₹ ••••••"
                    } else {
                        when (transaction.type) {
                            TransactionType.Expense ->
                                MoneyFormatter.formatMinorUnits(transaction.amount, showSign = true, isCredit = false)
                            TransactionType.Income, TransactionType.Refund ->
                                MoneyFormatter.formatMinorUnits(transaction.amount, showSign = true, isCredit = true)
                            else ->
                                MoneyFormatter.formatMinorUnits(transaction.amount, showSign = false)
                        }
                    }

                Text(
                    text = amountFormatted,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = merchantDisplay,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Low confidence warning pill if applicable
                val parseConfidence = transaction.parseConfidence
                if (parseConfidence != null && parseConfidence < 0.70f) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val pct = (parseConfidence * 100).toInt().coerceIn(0, 100)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.financialColors.warningContainer,
                    ) {
                        Text(
                            text = stringResource(id = R.string.review_low_confidence_badge, pct),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.financialColors.warning,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // Raw SMS Snippet if present
                val rawSms = transaction.note
                if (!rawSms.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(id = R.string.review_original_sms),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rawSms.take(160) + if (rawSms.length > 160) "…" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Category Suggestion Chips
                Text(
                    text = stringResource(id = R.string.review_select_category),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestedCategories.forEach { category ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAcceptCategory(category, rememberRule)
                            },
                            label = { Text(text = category) },
                            leadingIcon = { CategoryBadge(categoryName = category, size = 18.dp) },
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        )
                    }

                    FilterChip(
                        selected = false,
                        onClick = onOpenCategoryPicker,
                        label = { Text(text = stringResource(id = R.string.review_other_category)) },
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
            }
        }

        // Remember rule checkbox
        val merchantName = transaction.merchantNormalised ?: transaction.merchantRaw
        val ruleMessage =
            if (merchantName != null) {
                stringResource(id = R.string.review_remember_rule, merchantName)
            } else {
                stringResource(id = R.string.review_remember_rule_generic)
            }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { rememberRule = !rememberRule }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = rememberRule,
                onCheckedChange = { rememberRule = it },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ruleMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Action Buttons: Not an Expense & Skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onIgnore,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.review_not_an_expense),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.financialColors.expense,
                )
            }

            Button(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.review_skip_for_now),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ReviewCompletionContent(
    reviewedCount: Int,
    rulesLearnedCount: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.financialColors.income,
            modifier = Modifier.size(76.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.review_completion_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.review_completion_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (reviewedCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text =
                        stringResource(
                            id = R.string.review_completion_stats,
                            reviewedCount,
                            rulesLearnedCount,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.review_done_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewCategoryPickerSheet(
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCategories =
        remember(searchQuery) {
            if (searchQuery.isBlank()) {
                DEFAULT_CATEGORIES
            } else {
                DEFAULT_CATEGORIES.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(id = R.string.review_category_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = stringResource(id = R.string.review_category_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredCategories) { category ->
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategorySelected(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryBadge(categoryName = category, size = 32.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun extractSuggestedCategories(transaction: Transaction): List<String> {
    val existing = transaction.categorySource?.takeIf { it.isNotBlank() && !it.equals("Other", ignoreCase = true) }
    val common = listOf("Food & Dining", "Groceries", "Shopping", "Transport", "Bills & Utilities")
    val list = mutableListOf<String>()
    if (existing != null) {
        list.add(existing)
    }
    for (cat in common) {
        if (!list.contains(cat)) {
            list.add(cat)
        }
        if (list.size >= 4) break
    }
    return list
}
