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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.feature.transactions.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetBudgetSheet(
    onSaveBudget: (category: String?, budgetMinor: Long) -> Unit,
    onDismiss: () -> Unit,
    targetCategory: String? = null,
    currentBudgetMinor: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountInput by remember(currentBudgetMinor) {
        mutableStateOf(BudgetConversionHelper.formatMinorToInput(currentBudgetMinor))
    }

    val title =
        if (targetCategory != null) {
            stringResource(id = R.string.budget_title_category, targetCategory)
        } else {
            stringResource(id = R.string.budget_title_monthly)
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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    amountInput = filtered
                },
                label = { Text(text = stringResource(id = R.string.budget_amount_label)) },
                prefix = {
                    Text(
                        text = "₹ ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.income,
                    )
                },
                placeholder = { Text(text = "0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.financialColors.income,
                        focusedLabelColor = MaterialTheme.financialColors.income,
                        cursorColor = MaterialTheme.financialColors.income,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BudgetConversionHelper.PRESET_BUDGETS_MINOR.forEach { presetMinor ->
                    val formattedChip =
                        MoneyFormatter.formatMinorUnits(presetMinor).substringBefore(".")
                    val isSelected =
                        BudgetConversionHelper.parseInputToMinor(amountInput) == presetMinor

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            amountInput = BudgetConversionHelper.formatMinorToInput(presetMinor)
                        },
                        label = { Text(text = formattedChip) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.financialColors.income.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.financialColors.income,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions Row: Cancel & Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.budget_cancel_action),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val parsedMinor = BudgetConversionHelper.parseInputToMinor(amountInput)
                        onSaveBudget(targetCategory, parsedMinor)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.income,
                            contentColor = Color(0xFF0F1B12),
                        ),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.budget_save_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
