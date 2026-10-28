package com.kharcha.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.component.BudgetProgressBar
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.model.Transaction
import java.time.LocalDate

@Composable
fun BudgetsTabContent(
    transactions: List<Transaction>,
    monthlyBudgetMinor: Long,
    onUpdateMonthlyBudget: (Long) -> Unit,
    categoryShares: List<CategorySpendShare>,
    modifier: Modifier = Modifier,
    referenceDate: LocalDate = LocalDate.now(),
) {
    val totalDaysInMonth = referenceDate.lengthOfMonth()
    val currentDay = referenceDate.dayOfMonth
    val daysRemaining = maxOf(1, totalDaysInMonth - currentDay + 1)

    val safeToSpend =
        remember(transactions, monthlyBudgetMinor, referenceDate) {
            InsightsAnalyticsHelper.calculateSafeToSpend(
                transactions = transactions,
                referenceDate = referenceDate,
                customMonthlyBudgetMinor = monthlyBudgetMinor,
            )
        }

    val cumulativeSpend =
        remember(transactions, referenceDate) {
            calculateCumulativeSpendByDay(transactions, referenceDate)
        }

    var showEditBudgetDialog by remember { mutableStateOf(false) }
    val isPrivacy = LocalPrivacyMode.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Overall Monthly Budget Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.insights_budgets_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(id = R.string.insights_budgets_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = { showEditBudgetDialog = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.budget_edit_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val spentFormatted =
                    if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(safeToSpend.totalSpentMinor)
                val budgetFormatted =
                    if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(monthlyBudgetMinor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.insights_budget_spent, spentFormatted, budgetFormatted),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    PacingStatusBadge(status = safeToSpend.pacingStatus)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Traffic-Light Progress Bar
                BudgetProgressBar(
                    spentMinor = safeToSpend.totalSpentMinor,
                    budgetMinor = monthlyBudgetMinor,
                    daysRemaining = daysRemaining,
                    height = 10.dp,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Burn & Safe Allowance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val dailyBurnStr =
                        if (isPrivacy) {
                            "₹ ••••••"
                        } else {
                            MoneyFormatter.formatMinorUnits(safeToSpend.currentDailyBurnMinor)
                        }
                    val dailySafeStr =
                        if (isPrivacy) {
                            "₹ ••••••"
                        } else {
                            MoneyFormatter.formatMinorUnits(safeToSpend.dailySafeMinor)
                        }

                    Text(
                        text = stringResource(id = R.string.insights_daily_burn_rate, dailyBurnStr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Safe allowance: $dailySafeStr/day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // 2. Spend Pacing Sparkline
        SpendPacingSparkline(
            cumulativeSpend = cumulativeSpend,
            monthlyBudgetMinor = monthlyBudgetMinor,
            totalDaysInMonth = totalDaysInMonth,
            currentDay = currentDay,
        )

        // 3. Category Budgets Section
        if (categoryShares.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(id = R.string.budget_category_limits_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                categoryShares.forEach { share ->
                    // Baseline category budget allocation: proportion of total budget or 25% minimum
                    val allocatedBudget =
                        maxOf(
                            (monthlyBudgetMinor * 25) / 100,
                            (monthlyBudgetMinor * (share.percentage * 1.2f).toLong()),
                        )

                    CategoryBudgetCard(
                        categoryName = share.categoryName,
                        spentMinor = share.totalMinor,
                        budgetMinor = allocatedBudget,
                        daysRemaining = daysRemaining,
                        isPrivacy = isPrivacy,
                    )
                }
            }
        }
    }

    // Edit Budget Dialog
    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudgetMinor = monthlyBudgetMinor,
            onSave = { newBudget ->
                onUpdateMonthlyBudget(newBudget)
                showEditBudgetDialog = false
            },
            onDismiss = { showEditBudgetDialog = false },
        )
    }
}

@Composable
private fun CategoryBudgetCard(
    categoryName: String,
    spentMinor: Long,
    budgetMinor: Long,
    daysRemaining: Int,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    CategoryBadge(categoryName = categoryName, size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val spentStr = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(spentMinor)
                val budgetStr = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(budgetMinor)
                Text(
                    text = "$spentStr / $budgetStr",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Traffic-light progress bar
            BudgetProgressBar(
                spentMinor = spentMinor,
                budgetMinor = budgetMinor,
                daysRemaining = daysRemaining,
                height = 6.dp,
            )
        }
    }
}

@Composable
private fun EditBudgetDialog(
    currentBudgetMinor: Long,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var textValue by remember {
        val inRupees = currentBudgetMinor / 100
        mutableStateOf(inRupees.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.budget_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            textValue = input
                        }
                    },
                    prefix = { Text(text = "₹") },
                    placeholder = { Text(text = stringResource(id = R.string.budget_edit_amount_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rupees = textValue.toLongOrNull() ?: (currentBudgetMinor / 100)
                    onSave(rupees * 100)
                },
            ) {
                Text(text = stringResource(id = R.string.budget_edit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.budget_edit_cancel))
            }
        },
    )
}
