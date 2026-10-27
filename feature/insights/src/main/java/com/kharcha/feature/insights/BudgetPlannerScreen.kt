package com.kharcha.feature.insights

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.BudgetGauge
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.theme.financialColors

private val ALL_PLANNER_CATEGORIES =
    listOf(
        "Shopping",
        "Food & Drinks",
        "Groceries",
        "Transport",
        "Bills & Utilities",
        "Entertainment",
        "Health",
        "Investments",
        "Others",
    )

/**
 * Budget Planner Screen (UI Spec Section 7.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    currentMonthlyBudgetMinor: Long = 0L,
    categoryBudgets: Map<String, Long> = emptyMap(),
    onSaveBudgets: (overallBudgetMinor: Long, categoryBudgets: Map<String, Long>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overallBudgetInput by remember(currentMonthlyBudgetMinor) {
        val rupees = if (currentMonthlyBudgetMinor > 0L) (currentMonthlyBudgetMinor / 100).toString() else ""
        mutableStateOf(rupees)
    }
    var showOverallBudgetDialog by remember { mutableStateOf(false) }

    val activeCategoryBudgets =
        remember(categoryBudgets) {
            mutableStateMapOf<String, String>().apply {
                categoryBudgets.forEach { (cat, minor) ->
                    this[cat] = (minor / 100).toString()
                }
            }
        }

    var showSelectCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.budget_planner_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.navigate_up),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Button(
                        onClick = {
                            val overallMinor = (overallBudgetInput.toLongOrNull() ?: 0L) * 100L
                            val mappedCategoryBudgets =
                                activeCategoryBudgets.mapNotNull { (cat, str) ->
                                    val minor = (str.toLongOrNull() ?: 0L) * 100L
                                    if (minor > 0L) cat to minor else null
                                }.toMap()
                            onSaveBudgets(overallMinor, mappedCategoryBudgets)
                            onBack()
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.limeAccent,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            ),
                        shape = RoundedCornerShape(100.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.budget_planner_set_budget_button),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Header Gauge Card
            item {
                BudgetGauge(
                    modifier = Modifier.padding(top = 4.dp),
                    fillFraction = 0.65f,
                )
            }

            // 2. Overall Budget Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.financialColors.chartBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_category_bills),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.chartBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(id = R.string.budget_planner_overall_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }

                        val overallValueText =
                            if (overallBudgetInput.isNotBlank()) {
                                "₹" + overallBudgetInput
                            } else {
                                stringResource(id = R.string.budget_planner_set_now)
                            }

                        Text(
                            text = overallValueText,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.Underline,
                                ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.limeAccent,
                            modifier = Modifier.clickable { showOverallBudgetDialog = true },
                        )
                    }
                }
            }

            // 3. Category Wise Budget Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.budget_planner_category_wise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                    Text(
                        text = stringResource(id = R.string.budget_planner_add_category),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.limeAccent,
                        modifier = Modifier.clickable { showSelectCategoryDialog = true },
                    )
                }
            }

            // 4. Added Categories Rows
            if (activeCategoryBudgets.isEmpty()) {
                item {
                    Text(
                        text = "No category budgets set yet. Tap '+ Add category' above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            } else {
                items(activeCategoryBudgets.keys.toList()) { category ->
                    val budgetStr = activeCategoryBudgets[category] ?: ""
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.financialColors.cardBackground,
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                CategoryBadge(categoryName = category, size = 32.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.financialColors.textPrimary,
                                )
                            }

                            // White pill input on right ("₹0" and clear "×")
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.financialColors.raisedRowBackground,
                                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                modifier = Modifier.width(110.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "₹",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    BasicTextField(
                                        value = budgetStr,
                                        onValueChange = { newVal ->
                                            if (newVal.all { it.isDigit() }) {
                                                activeCategoryBudgets[category] = newVal
                                            }
                                        },
                                        textStyle =
                                            TextStyle(
                                                color = MaterialTheme.financialColors.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        cursorBrush = SolidColor(MaterialTheme.financialColors.limeAccent),
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { activeCategoryBudgets.remove(category) },
                                        modifier = Modifier.size(18.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.financialColors.textSecondary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showOverallBudgetDialog) {
        var tempInput by remember { mutableStateOf(overallBudgetInput) }
        AlertDialog(
            onDismissRequest = { showOverallBudgetDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.budget_planner_overall_label),
                    color = MaterialTheme.financialColors.textPrimary,
                )
            },
            text = {
                OutlinedTextField(
                    value = tempInput,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) tempInput = it },
                    label = { Text("Amount in ₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        overallBudgetInput = tempInput
                        showOverallBudgetDialog = false
                    },
                ) {
                    Text("Done", color = MaterialTheme.financialColors.limeAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverallBudgetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.financialColors.textSecondary)
                }
            },
            containerColor = MaterialTheme.financialColors.cardBackground,
        )
    }

    if (showSelectCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showSelectCategoryDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.budget_planner_select_category),
                    color = MaterialTheme.financialColors.textPrimary,
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(ALL_PLANNER_CATEGORIES) { cat ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!activeCategoryBudgets.containsKey(cat)) {
                                            activeCategoryBudgets[cat] = "0"
                                        }
                                        showSelectCategoryDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryBadge(categoryName = cat, size = 32.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.financialColors.hairlineDivider,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectCategoryDialog = false }) {
                    Text("Cancel", color = MaterialTheme.financialColors.textSecondary)
                }
            },
            containerColor = MaterialTheme.financialColors.cardBackground,
        )
    }
}
