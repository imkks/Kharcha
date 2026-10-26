package com.kharcha.core.designsystem.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kharcha.core.common.DateTimeFormatterHelper
import com.kharcha.core.common.LocationCaptureHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val EXPENSE_CATEGORIES =
    listOf(
        "Not Categorised",
        "Food & Drinks",
        "Groceries",
        "Shopping",
        "Transport",
        "Bills & Utilities",
        "Entertainment",
        "Health",
        "Investments",
        "Others",
    )

private val INCOME_CATEGORIES =
    listOf(
        "Salary",
        "Investments",
        "Transfer",
        "Others",
    )

/**
 * Full-screen Axio-style Add Transaction Form (UI Spec 7.4).
 * Opened from the lime "+ Add" pill on Home.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account> = emptyList(),
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var switchOption by remember { mutableStateOf(TransactionTypeSwitchOption.SPEND) }

    var amountInput by remember { mutableStateOf("") }
    var payeeInput by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var selectedAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var showAccountSheet by remember { mutableStateOf(false) }

    var selectedCategory by remember(switchOption) {
        mutableStateOf(
            if (switchOption == TransactionTypeSwitchOption.SPEND) "Not Categorised" else "Salary",
        )
    }
    var isCategoryPickerExpanded by remember { mutableStateOf(false) }

    var isExtrasExpanded by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }
    val tagsList = remember { mutableStateListOf<String>() }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            receiptUri = uri?.toString()
        }

    val amountMinor = MoneyFormatter.parseMinorUnits(amountInput)
    val isSaveEnabled = amountMinor != null && amountMinor > 0L

    val formattedDateTime =
        remember(selectedTimestamp) {
            val zdt = Instant.ofEpochMilli(selectedTimestamp).atZone(ZoneId.systemDefault())
            val today = LocalDate.now(ZoneId.systemDefault())
            val timeStr = zdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
            if (zdt.toLocalDate() == today) {
                "Today, $timeStr"
            } else {
                DateTimeFormatterHelper.formatDetailDateTime(selectedTimestamp)
            }
        }

    fun openDateTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        selectedTimestamp = cal.timeInMillis
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false,
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun buildTransaction(): Transaction? {
        val validAmount = amountMinor ?: return null
        val txnType =
            if (switchOption == TransactionTypeSwitchOption.SPEND) {
                TransactionType.Expense
            } else {
                TransactionType.Income
            }

        val location = LocationCaptureHelper.getLastKnownLocation(context)

        val noteTagsSuffix =
            if (tagsList.isNotEmpty()) {
                tagsList.joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
            } else {
                ""
            }
        val fullNote =
            when {
                noteInput.isNotBlank() && noteTagsSuffix.isNotBlank() -> "$noteInput $noteTagsSuffix"
                noteInput.isNotBlank() -> noteInput
                noteTagsSuffix.isNotBlank() -> noteTagsSuffix
                else -> null
            }

        val channel =
            when (selectedAccount?.type) {
                AccountType.CreditCard -> Channel.CARD
                AccountType.Wallet -> Channel.WALLET
                else -> Channel.UPI
            }

        return Transaction(
            amount = validAmount,
            type = txnType,
            channel = channel,
            dateTime = selectedTimestamp,
            merchantRaw = payeeInput.ifBlank { selectedCategory },
            categorySource = selectedCategory,
            accountId = selectedAccount?.id ?: 0L,
            note = fullNote,
            latitude = location?.latitude,
            longitude = location?.longitude,
            locationLabel = location?.label,
            isReviewed = true,
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.add_txn_fullscreen_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.clear_search),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    FormFooter(
                        onSaveAndAddAnother = {
                            val txn = buildTransaction()
                            if (txn != null) {
                                onSave(txn)
                                amountInput = ""
                                payeeInput = ""
                                noteInput = ""
                                tagsList.clear()
                                receiptUri = null
                            }
                        },
                        onSave = {
                            val txn = buildTransaction()
                            if (txn != null) {
                                onSave(txn)
                                onDismiss()
                            }
                        },
                        isSaveEnabled = isSaveEnabled,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                // 1. Type Switch (Spend vs Income)
                TypeSwitch(
                    selectedOption = switchOption,
                    onOptionSelected = { option ->
                        switchOption = option
                        selectedCategory =
                            if (option == TransactionTypeSwitchOption.SPEND) {
                                "Food & Drinks"
                            } else {
                                "Salary"
                            }
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Amount Card (Amount spent, Enter amount, Date & time, Paid to)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Caption
                        Text(
                            text =
                                if (switchOption == TransactionTypeSwitchOption.SPEND) {
                                    stringResource(id = R.string.add_txn_amount_spent)
                                } else {
                                    stringResource(id = R.string.add_txn_amount_credited)
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Amount row with ₹ and arrow
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "₹ ",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (amountInput.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.add_txn_enter_amount),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.financialColors.textSecondary.copy(alpha = 0.5f),
                                    )
                                }
                                BasicTextField(
                                    value = amountInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() || it == '.' }) {
                                            amountInput = input
                                        }
                                    },
                                    textStyle =
                                        TextStyle(
                                            color = MaterialTheme.financialColors.textPrimary,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    cursorBrush = SolidColor(MaterialTheme.financialColors.limeAccent),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_outward),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.limeAccent,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.financialColors.hairlineDivider,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Date & time row
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { openDateTimePicker() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_txn_date_time),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                            Text(
                                text = formattedDateTime,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.financialColors.hairlineDivider,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Paid to / Received from row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text =
                                    if (switchOption == TransactionTypeSwitchOption.SPEND) {
                                        stringResource(id = R.string.add_txn_paid_to)
                                    } else {
                                        stringResource(id = R.string.add_txn_received_from)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.financialColors.textSecondary,
                                modifier = Modifier.width(110.dp),
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (payeeInput.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.add_txn_payee_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.financialColors.textSecondary.copy(alpha = 0.5f),
                                    )
                                }
                                BasicTextField(
                                    value = payeeInput,
                                    onValueChange = { payeeInput = it },
                                    textStyle =
                                        TextStyle(
                                            color = MaterialTheme.financialColors.textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    cursorBrush = SolidColor(MaterialTheme.financialColors.limeAccent),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Account Row
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.financialColors.raisedRowBackground,
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showAccountSheet = true },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bank),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.limeAccent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val accountTitle =
                                selectedAccount?.let { acc ->
                                    val bank = acc.bankName ?: acc.name
                                    val last4 = acc.last4?.let { " - $it" } ?: ""
                                    "$bank$last4"
                                } ?: stringResource(id = R.string.add_txn_select_account)
                            Text(
                                text = accountTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Category Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_txn_category_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                            if (!isCategoryPickerExpanded) {
                                Text(
                                    text = stringResource(id = R.string.add_txn_edit_category),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.limeAccent,
                                    modifier = Modifier.clickable { isCategoryPickerExpanded = true },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val availableCategories =
                            if (switchOption == TransactionTypeSwitchOption.SPEND) {
                                EXPENSE_CATEGORIES
                            } else {
                                INCOME_CATEGORIES
                            }

                        if (!isCategoryPickerExpanded) {
                            // Single selected category row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                CategoryBadge(categoryName = selectedCategory, size = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = selectedCategory,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.financialColors.textPrimary,
                                )
                            }
                        } else {
                            // 3-column category grid
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                availableCategories.chunked(3).forEach { rowCats ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowCats.forEach { cat ->
                                            val isSelected = cat == selectedCategory
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color =
                                                    if (isSelected) {
                                                        MaterialTheme.financialColors.limeAccent.copy(alpha = 0.2f)
                                                    } else {
                                                        MaterialTheme.financialColors.raisedRowBackground
                                                    },
                                                border =
                                                    if (isSelected) {
                                                        BorderStroke(1.5.dp, MaterialTheme.financialColors.limeAccent)
                                                    } else {
                                                        BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider)
                                                    },
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            selectedCategory = cat
                                                            isCategoryPickerExpanded = false
                                                        },
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    CategoryBadge(categoryName = cat, size = 32.dp)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = cat,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center,
                                                        color = MaterialTheme.financialColors.textPrimary,
                                                    )
                                                }
                                            }
                                        }
                                        // Fill remaining slots in incomplete row
                                        repeat(3 - rowCats.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Collapsed Extras Accordion (Notes, Tags, Receipt)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.financialColors.cardBackground,
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isExtrasExpanded = !isExtrasExpanded },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_txn_collapsed_extras),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.limeAccent,
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        AnimatedVisibility(visible = isExtrasExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                // Notes
                                Text(
                                    text = stringResource(id = R.string.add_txn_notes_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = noteInput,
                                    onValueChange = { noteInput = it },
                                    placeholder = {
                                        Text(
                                            text = stringResource(id = R.string.add_txn_notes_hint),
                                            color = MaterialTheme.financialColors.textSecondary.copy(alpha = 0.5f),
                                        )
                                    },
                                    singleLine = true,
                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.financialColors.limeAccent,
                                            unfocusedBorderColor = MaterialTheme.financialColors.hairlineDivider,
                                            focusedTextColor = MaterialTheme.financialColors.textPrimary,
                                            unfocusedTextColor = MaterialTheme.financialColors.textPrimary,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Tags
                                Text(
                                    text = stringResource(id = R.string.add_txn_tags_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    tagsList.forEach { tag ->
                                        TagChip(
                                            tag = tag,
                                            isSelected = true,
                                            onClick = { tagsList.remove(tag) },
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.financialColors.cardBackground,
                                        border = BorderStroke(1.dp, MaterialTheme.financialColors.limeAccent),
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .clickable { showAddTagDialog = true },
                                    ) {
                                        Text(
                                            text = "+ Add tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.financialColors.limeAccent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Receipt
                                Text(
                                    text = stringResource(id = R.string.add_txn_receipt_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.financialColors.raisedRowBackground,
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { photoPickerLauncher.launch("image/*") },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_receipt_photo),
                                            contentDescription = null,
                                            tint = MaterialTheme.financialColors.limeAccent,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text =
                                                if (receiptUri != null) {
                                                    "Receipt attached"
                                                } else {
                                                    stringResource(id = R.string.add_txn_attach_receipt)
                                                },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.financialColors.textPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAccountSheet) {
        SelectAccountSheet(
            accounts = accounts,
            selectedAccountId = selectedAccount?.id,
            onAccountSelected = { acc ->
                selectedAccount = acc
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false },
        )
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = {
                Text(
                    text = "Add tag",
                    color = MaterialTheme.financialColors.textPrimary,
                )
            },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("#online, #dinner") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newTagInput.trim().removePrefix("#")
                        if (clean.isNotBlank() && !tagsList.contains(clean)) {
                            tagsList.add(clean)
                        }
                        newTagInput = ""
                        showAddTagDialog = false
                    },
                ) {
                    Text("Add", color = MaterialTheme.financialColors.limeAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancel", color = MaterialTheme.financialColors.textSecondary)
                }
            },
            containerColor = MaterialTheme.financialColors.cardBackground,
        )
    }
}
