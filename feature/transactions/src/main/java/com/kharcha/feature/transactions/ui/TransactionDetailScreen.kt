package com.kharcha.feature.transactions.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.CategoryIconHelper
import com.kharcha.core.designsystem.component.DEFAULT_CATEGORIES
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    onBack: () -> Unit,
    onSave: (updated: Transaction, propagateToMerchant: Boolean) -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.categorySource ?: "Other") }
    var noteText by remember { mutableStateOf(transaction.note ?: "") }
    var isRawSmsExpanded by remember { mutableStateOf(false) }

    var showCategorySheet by remember { mutableStateOf(false) }
    var showPropagateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var propagateToAllMerchants by remember { mutableStateOf(true) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isPrivacy = LocalPrivacyMode.current

    val merchantName =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: stringResource(id = DesignR.string.unknown_merchant)

    val formattedDate = remember(transaction.dateTime) {
        val instant = Instant.ofEpochMilli(transaction.dateTime)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    }

    val typeColor = when (selectedType) {
        TransactionType.Income -> MaterialTheme.financialColors.income
        TransactionType.Expense -> MaterialTheme.financialColors.expense
        TransactionType.Transfer -> MaterialTheme.financialColors.transfer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.txn_detail_close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.detail_action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Button(
                        onClick = {
                            val categoryChanged = selectedCategory != (transaction.categorySource ?: "Other")
                            if (categoryChanged && !merchantName.equals("Unknown", ignoreCase = true)) {
                                showPropagateDialog = true
                            } else {
                                val updated = transaction.copy(
                                    type = selectedType,
                                    categorySource = selectedCategory,
                                    note = noteText.takeIf { it.isNotBlank() },
                                    isReviewed = true,
                                )
                                onSave(updated, false)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = stringResource(id = R.string.detail_save_changes))
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Hero Card: Amount, Merchant, Date, Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CategoryBadge(
                            categoryName = selectedCategory,
                            size = 56.dp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val displayAmount = if (isPrivacy) {
                            "₹ ••••••"
                        } else {
                            val sign = if (selectedType == TransactionType.Income) "+" else "-"
                            sign + MoneyFormatter.formatMinorUnits(transaction.amount)
                        }

                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = merchantName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Review Status Indicator Pill
                        Surface(
                            shape = CircleShape,
                            color = if (transaction.isReviewed) {
                                MaterialTheme.financialColors.income.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.financialColors.warning.copy(alpha = 0.15f)
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (transaction.isReviewed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.financialColors.income,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.detail_status_verified),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.financialColors.income,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.detail_status_needs_review),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.financialColors.warning,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Type & Category Edit Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple(
                            TransactionType.Expense,
                            stringResource(id = DesignR.string.add_txn_type_expense),
                            DesignR.drawable.ic_trending_down,
                        ),
                        Triple(
                            TransactionType.Income,
                            stringResource(id = DesignR.string.add_txn_type_income),
                            DesignR.drawable.ic_trending_up,
                        ),
                        Triple(
                            TransactionType.Transfer,
                            stringResource(id = DesignR.string.add_txn_type_transfer),
                            DesignR.drawable.ic_category_transfer,
                        ),
                    ).forEach { (type, label, iconRes) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == TransactionType.Transfer) {
                                    selectedCategory = "Transfer"
                                }
                            },
                            label = { Text(text = label) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Category Selector Field
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCategorySheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(
                                categoryName = selectedCategory,
                                size = 32.dp,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.detail_category_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = selectedCategory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Details Metadata Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.detail_section_details),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Channel
                        MetadataRow(
                            label = stringResource(id = R.string.detail_channel),
                            value = transaction.channel?.name ?: "SMS",
                        )

                        // Reference Number with Copy
                        transaction.refNumber?.let { ref ->
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.detail_reference),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = ref,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                val copiedMsg = stringResource(id = R.string.detail_copied)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(ref))
                                        scope.launch { snackbarHostState.showSnackbar(copiedMsg) }
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        // UPI ID
                        transaction.upiId?.let { upi ->
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            MetadataRow(
                                label = stringResource(id = R.string.detail_upi_id),
                                value = upi,
                            )
                        }

                        // Balance After
                        transaction.balanceAfter?.let { bal ->
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            val balFormatted = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(bal)
                            MetadataRow(
                                label = stringResource(id = R.string.detail_balance_after),
                                value = balFormatted,
                            )
                        }

                        // Parse Confidence
                        transaction.parseConfidence?.let { conf ->
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            val pct = (conf * 100).toInt().coerceIn(0, 100)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_parse_confidence),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { conf },
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (conf >= 0.8f) {
                                            MaterialTheme.financialColors.income
                                        } else {
                                            MaterialTheme.financialColors.warning
                                        },
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Note Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.detail_section_note),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text(text = stringResource(id = R.string.detail_note_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3,
                        )
                    }
                }
            }

            // Original SMS Expandable Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isRawSmsExpanded = !isRawSmsExpanded },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.detail_original_sms),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = if (isRawSmsExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        AnimatedVisibility(visible = isRawSmsExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                val smsText = transaction.note?.takeIf { it.isNotBlank() }
                                    ?: stringResource(id = R.string.detail_no_sms)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = smsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val reportSentMsg = stringResource(id = R.string.detail_report_sent)
                                OutlinedButton(
                                    onClick = {
                                        scope.launch { snackbarHostState.showSnackbar(reportSentMsg) }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.detail_report_wrong_parse),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Category Picker Bottom Sheet
    if (showCategorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CategoryPickerSheetContent(
                selectedCategory = selectedCategory,
                onCategorySelected = { cat ->
                    selectedCategory = cat
                    if (cat == "Transfer") {
                        selectedType = TransactionType.Transfer
                    }
                    showCategorySheet = false
                },
                onDismiss = { showCategorySheet = false },
            )
        }
    }

    // Merchant Rule Propagation Dialog
    if (showPropagateDialog) {
        AlertDialog(
            onDismissRequest = { showPropagateDialog = false },
            title = { Text(text = stringResource(id = R.string.detail_propagate_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            id = R.string.detail_propagate_dialog_message,
                            merchantName,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { propagateToAllMerchants = true },
                    ) {
                        RadioButton(
                            selected = propagateToAllMerchants,
                            onClick = { propagateToAllMerchants = true },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.detail_propagate_option_all),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { propagateToAllMerchants = false },
                    ) {
                        RadioButton(
                            selected = !propagateToAllMerchants,
                            onClick = { propagateToAllMerchants = false },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.detail_propagate_option_single),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPropagateDialog = false
                        val updated = transaction.copy(
                            type = selectedType,
                            categorySource = selectedCategory,
                            note = noteText.takeIf { it.isNotBlank() },
                            isReviewed = true,
                        )
                        onSave(updated, propagateToAllMerchants)
                    },
                ) {
                    Text(text = stringResource(id = R.string.detail_propagate_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPropagateDialog = false }) {
                    Text(text = stringResource(id = R.string.detail_propagate_cancel))
                }
            },
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(id = R.string.detail_delete_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.detail_delete_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(transaction)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(text = stringResource(id = R.string.detail_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(id = R.string.detail_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun CategoryPickerSheetContent(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            DEFAULT_CATEGORIES
        } else {
            DEFAULT_CATEGORIES.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(id = R.string.detail_category_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(text = stringResource(id = R.string.detail_category_search_hint)) },
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
                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCategorySelected(category) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(categoryName = category, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
