package com.kharcha.feature.transactions.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.AmountRangeFilter
import com.kharcha.core.model.Channel
import com.kharcha.core.model.DateRangeSelection
import com.kharcha.core.model.ReviewStatusFilter
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

private enum class FilterDateOption {
    CUSTOM,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    ALL_TIME,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    @Suppress("UNUSED_PARAMETER") transactions: List<Transaction>,
    initialFilter: TransactionFilter,
    onApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var draftFilter by remember { mutableStateOf(initialFilter) }

    val zoneId = remember { ZoneId.systemDefault() }
    val now = remember { LocalDate.now(zoneId) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()) }

    var customStartDate by remember {
        val initialStart =
            when (val dr = initialFilter.dateRange) {
                is DateRangeSelection.Custom ->
                    Instant.ofEpochMilli(dr.startEpochMs).atZone(zoneId).toLocalDate()
                else -> now.withDayOfMonth(1)
            }
        mutableStateOf(initialStart)
    }

    var customEndDate by remember {
        val initialEnd =
            when (val dr = initialFilter.dateRange) {
                is DateRangeSelection.Custom ->
                    Instant.ofEpochMilli(dr.endEpochMs).atZone(zoneId).toLocalDate()
                else -> now
            }
        mutableStateOf(initialEnd)
    }

    var selectedDateOption by remember {
        val initial =
            when (val dr = initialFilter.dateRange) {
                is DateRangeSelection.Custom -> FilterDateOption.CUSTOM
                is DateRangeSelection.LastNDays ->
                    if (dr.days == 180) FilterDateOption.LAST_6_MONTHS else FilterDateOption.LAST_3_MONTHS
                is DateRangeSelection.AllTime -> FilterDateOption.ALL_TIME
                else -> FilterDateOption.CUSTOM
            }
        mutableStateOf(initial)
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    val radioColors =
        RadioButtonDefaults.colors(
            selectedColor = MaterialTheme.financialColors.income,
            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
        ) {
            // Title: Filter
            Text(
                text = stringResource(id = R.string.filter_modal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
            ) {
                // Option 1: Custom Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedDateOption = FilterDateOption.CUSTOM }
                            .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = selectedDateOption == FilterDateOption.CUSTOM,
                        onClick = { selectedDateOption = FilterDateOption.CUSTOM },
                        colors = radioColors,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.filter_custom_date),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight =
                            if (selectedDateOption == FilterDateOption.CUSTOM) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                    )
                }

                // Nested Date Selectors when Custom Date is selected
                AnimatedVisibility(visible = selectedDateOption == FilterDateOption.CUSTOM) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Start Date Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showStartDatePicker = true },
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_calendar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.filter_start_date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = customStartDate.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // End Date Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showEndDatePicker = true },
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_calendar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.filter_end_date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = customEndDate.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // Option 2: Last 3 months
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedDateOption = FilterDateOption.LAST_3_MONTHS }
                            .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = selectedDateOption == FilterDateOption.LAST_3_MONTHS,
                        onClick = { selectedDateOption = FilterDateOption.LAST_3_MONTHS },
                        colors = radioColors,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.filter_last_3_months),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight =
                            if (selectedDateOption == FilterDateOption.LAST_3_MONTHS) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                    )
                }

                // Option 3: Last 6 months
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedDateOption = FilterDateOption.LAST_6_MONTHS }
                            .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = selectedDateOption == FilterDateOption.LAST_6_MONTHS,
                        onClick = { selectedDateOption = FilterDateOption.LAST_6_MONTHS },
                        colors = radioColors,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.filter_last_6_months),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight =
                            if (selectedDateOption == FilterDateOption.LAST_6_MONTHS) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                    )
                }

                // Option 4: All Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedDateOption = FilterDateOption.ALL_TIME }
                            .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = selectedDateOption == FilterDateOption.ALL_TIME,
                        onClick = { selectedDateOption = FilterDateOption.ALL_TIME },
                        colors = radioColors,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.filter_all_time),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight =
                            if (selectedDateOption == FilterDateOption.ALL_TIME) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Expandable More Filters (Type, Category, Amount)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showMoreOptions = !showMoreOptions }
                            .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = "More filters (Type, Category, Amount)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector =
                            if (showMoreOptions) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AnimatedVisibility(visible = showMoreOptions) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Transaction Type
                        SectionTitle(text = stringResource(id = R.string.filter_section_type))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val types =
                                listOf(
                                    TransactionType.EXPENSE to
                                        stringResource(id = R.string.transactions_filter_expense),
                                    TransactionType.INCOME to
                                        stringResource(id = R.string.transactions_filter_income),
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Multi-select
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount Range
                        SectionTitle(text = stringResource(id = R.string.filter_section_amount))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val amountOptions =
                                listOf(
                                    AmountRangeFilter.Any to stringResource(id = R.string.filter_amount_any),
                                    AmountRangeFilter.Under100 to
                                        stringResource(id = R.string.filter_amount_under_100),
                                    AmountRangeFilter.Between100And500 to
                                        stringResource(id = R.string.filter_amount_100_500),
                                    AmountRangeFilter.Between500And2000 to
                                        stringResource(id = R.string.filter_amount_500_2k),
                                    AmountRangeFilter.Between2000And10000 to
                                        stringResource(id = R.string.filter_amount_2k_10k),
                                    AmountRangeFilter.Above10000 to
                                        stringResource(id = R.string.filter_amount_above_10k),
                                )
                            amountOptions.forEach { (option, label) ->
                                FilterChip(
                                    selected = draftFilter.amountRange == option,
                                    onClick = { draftFilter = draftFilter.copy(amountRange = option) },
                                    label = { Text(text = label) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Actions: Clear Filter + Apply Filter
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        draftFilter = TransactionFilter()
                        selectedDateOption = FilterDateOption.ALL_TIME
                    },
                ) {
                    Text(
                        text = stringResource(id = R.string.filter_clear_action),
                        color = MaterialTheme.financialColors.income,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Button(
                    onClick = {
                        val computedDateRange: DateRangeSelection =
                            when (selectedDateOption) {
                                FilterDateOption.CUSTOM -> {
                                    val startMs =
                                        customStartDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                                    val endMs =
                                        customEndDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
                                    DateRangeSelection.Custom(startMs, endMs)
                                }
                                FilterDateOption.LAST_3_MONTHS -> DateRangeSelection.LastNDays(90)
                                FilterDateOption.LAST_6_MONTHS -> DateRangeSelection.LastNDays(180)
                                FilterDateOption.ALL_TIME -> DateRangeSelection.AllTime
                            }
                        val finalFilter = draftFilter.copy(dateRange = computedDateRange)
                        onApply(finalFilter)
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.income,
                            contentColor = Color(0xFF0F1B12),
                        ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(44.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.filter_apply_action),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    // Material 3 Date Picker Dialog for Start Date
    if (showStartDatePicker) {
        val initialMs = customStartDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            customStartDate = Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate()
                        }
                        showStartDatePicker = false
                    },
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(text = "Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Date Picker Dialog for End Date
    if (showEndDatePicker) {
        val initialMs = customEndDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            customEndDate = Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate()
                        }
                        showEndDatePicker = false
                    },
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(text = "Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
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
