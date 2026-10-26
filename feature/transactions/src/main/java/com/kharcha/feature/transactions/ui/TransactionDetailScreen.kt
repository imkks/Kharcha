package com.kharcha.feature.transactions.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionClassifierHelper
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.CategoryIconHelper
import com.kharcha.core.designsystem.component.DEFAULT_CATEGORIES
import com.kharcha.core.designsystem.theme.CategoryColors
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    onBack: () -> Unit,
    onSave: (updated: Transaction, propagateToMerchant: Boolean) -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    allTransactions: List<Transaction> = emptyList(),
    accounts: List<Account> = emptyList(),
    onSaveTransactions: ((updatedList: List<Transaction>, updateMerchantRule: Boolean) -> Unit)? = null,
    onNavigateToTransaction: ((Long) -> Unit)? = null,
    onNavigateToMerchant: ((String) -> Unit)? = null,
    onLinkTransactions: ((primaryId: Long, secondaryId: Long, isRefund: Boolean) -> Unit)? = null,
    onUnlinkTransaction: ((id: Long) -> Unit)? = null,
) {
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.categorySource ?: "Other") }
    var noteText by remember { mutableStateOf(transaction.note ?: "") }

    var showMenu by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showPropagateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showSplitSheet by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showReceiptViewDialog by remember { mutableStateOf(false) }
    var showLinkSheet by remember { mutableStateOf(false) }

    val merchantName =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: stringResource(id = R.string.detail_original_sms)

    val isCreditTxn =
        selectedType == TransactionType.Income ||
            selectedType == TransactionType.Refund ||
            transaction.type == TransactionType.Income ||
            transaction.type == TransactionType.Refund

    val similarTransactions =
        remember(allTransactions, transaction, merchantName) {
            if (merchantName.equals("Unknown", ignoreCase = true) || merchantName.isBlank()) {
                emptyList()
            } else {
                allTransactions
                    .filter { other ->
                        other.id != transaction.id &&
                            (other.merchantNormalised.equals(merchantName, ignoreCase = true) ||
                                other.merchantRaw?.equals(merchantName, ignoreCase = true) == true)
                    }
                    .sortedByDescending { it.dateTime }
            }
        }

    var checkedSimilarIds by remember(similarTransactions) {
        mutableStateOf(similarTransactions.map { it.id }.toSet())
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isPrivacy = LocalPrivacyMode.current

    var receiptFile by remember(transaction.id) {
        mutableStateOf(ReceiptAttachmentHelper.getReceiptFile(context, transaction.id))
    }

    val receiptThumbnail = remember(receiptFile) {
        receiptFile?.let { ReceiptAttachmentHelper.loadBitmap(it, sampleSize = 4) }
    }

    val receiptFullBitmap = remember(receiptFile, showReceiptViewDialog) {
        if (showReceiptViewDialog && receiptFile != null) {
            ReceiptAttachmentHelper.loadBitmap(receiptFile!!, sampleSize = 1)
        } else {
            null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = ReceiptAttachmentHelper.saveReceipt(context, transaction.id, uri)
            receiptFile = saved
        }
    }

    val isSplitActive = remember(noteText) { SplitExpenseCalculator.isSplitActive(noteText) }
    val activeTags = remember(noteText, transaction.channel) {
        TagParser.extractTags(noteText, transaction.channel)
    }

    val visitsCount =
        remember(transaction, allTransactions) {
            if (allTransactions.isEmpty()) {
                1
            } else {
                allTransactions.count {
                    it.merchantNormalised == transaction.merchantNormalised ||
                        (it.merchantRaw != null && it.merchantRaw == transaction.merchantRaw)
                }.coerceAtLeast(1)
            }
        }

    val formattedDate =
        remember(transaction.dateTime) {
            val instant = Instant.ofEpochMilli(transaction.dateTime)
            val formatter = DateTimeFormatter.ofPattern("EEE, d MMM, h:mm a", Locale.getDefault())
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        }

    val linkedTxn =
        remember(transaction.linkedTransactionId, allTransactions) {
            transaction.linkedTransactionId?.let { id ->
                allTransactions.find { it.id == id }
            }
        }
    val linkedAccount =
        remember(linkedTxn, accounts) {
            linkedTxn?.let { lt -> accounts.find { it.id == lt.accountId } }
        }

    val topBarTitle =
        when (selectedType) {
            TransactionType.Expense -> stringResource(id = R.string.detail_debit_title)
            TransactionType.Income -> stringResource(id = R.string.detail_credit_title)
            TransactionType.Transfer -> stringResource(id = DesignR.string.add_txn_type_transfer)
            else -> stringResource(id = R.string.detail_title)
        }

    val hasPendingChanges =
        selectedType != transaction.type ||
            selectedCategory != (transaction.categorySource ?: "Other") ||
            noteText != (transaction.note ?: "")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.txn_detail_close),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(id = R.string.detail_action_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
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
            AnimatedVisibility(visible = hasPendingChanges) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        Button(
                            onClick = {
                                val categoryChanged =
                                    selectedCategory != (transaction.categorySource ?: "Other")
                                if (categoryChanged && similarTransactions.isNotEmpty()) {
                                    checkedSimilarIds = similarTransactions.map { it.id }.toSet()
                                    showPropagateDialog = true
                                } else {
                                    val updated =
                                        transaction.copy(
                                            type = selectedType,
                                            categorySource = selectedCategory,
                                            note = noteText.takeIf { it.isNotBlank() },
                                            isReviewed = true,
                                        )
                                    if (onSaveTransactions != null) {
                                        onSaveTransactions(listOf(updated), false)
                                    } else {
                                        onSave(updated, false)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.financialColors.income,
                                    contentColor = MaterialTheme.financialColors.onIncome,
                                ),
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.detail_save_changes),
                                fontWeight = FontWeight.Bold,
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Hero Card (Axio-style dark green gradient)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0F2615), MaterialTheme.financialColors.cardBackground),
                                    ),
                                )
                                .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Top row of Hero Card: Merchant name + Visit count pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = merchantName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textDecoration = TextDecoration.Underline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable(enabled = onNavigateToMerchant != null) {
                                        onNavigateToMerchant?.invoke(merchantName)
                                    },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.financialColors.cardBackground.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.detail_visits_count, visitsCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                    )
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_bar_chart),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.financialColors.income,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Amount with directional arrow
                        val displayAmount =
                            if (isPrivacy) {
                                "₹ ••••••"
                            } else {
                                MoneyFormatter.formatMinorUnits(transaction.amount)
                            }

                        val isCredit =
                            selectedType == TransactionType.Income || selectedType == TransactionType.Refund

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                                contentDescription = null,
                                tint = if (isCredit) MaterialTheme.financialColors.income else Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .then(if (isCredit) Modifier.rotate(180f) else Modifier),
                            )
                            Text(
                                text = displayAmount,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clickable Category Pill: [ 🛒 Groceries ]
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.financialColors.cardBackground,
                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                            modifier =
                                Modifier
                                    .clip(CircleShape)
                                    .clickable { showCategorySheet = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CategoryBadge(
                                    categoryName = selectedCategory,
                                    size = 20.dp,
                                )
                                Text(
                                    text = selectedCategory,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Date and Time
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9E9E),
                        )
                    }
                }
            }

            // 2. Account & Expense Toggle Card (Dark Navy #1F2D3D)
            item {
                val account = accounts.find { it.id == transaction.accountId }
                val isCardAccount =
                    account?.type == AccountType.CreditCard || transaction.channel == Channel.CARD
                val accountIcon =
                    if (isCardAccount) DesignR.drawable.ic_channel_card else DesignR.drawable.ic_bank
                val iconTint =
                    if (isCardAccount) {
                        MaterialTheme.financialColors.warning
                    } else {
                        MaterialTheme.financialColors.income
                    }
                val accountName =
                    account?.name ?: transaction.refNumber?.takeLast(4)?.let { "HDFC $it" }
                        ?: (transaction.channel?.name ?: "Bank Account")
                val accountTypeLabel =
                    if (isCardAccount) "Credit Card" else "Savings Account"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.accountNavy,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF131D28)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = accountIcon),
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = accountName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = accountTypeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E),
                                )
                            }
                        }

                        if (selectedType == TransactionType.Transfer) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.financialColors.transfer.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "Transfer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.financialColors.transfer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        } else if (isCreditTxn) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_account_income),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                Switch(
                                    checked = selectedType == TransactionType.Income,
                                    onCheckedChange = { isIncome ->
                                        selectedType =
                                            if (isIncome) {
                                                TransactionType.Income
                                            } else {
                                                TransactionType.Refund
                                            }
                                        val msg =
                                            if (isIncome) {
                                                context.getString(R.string.detail_toast_counted_income)
                                            } else {
                                                context.getString(R.string.detail_toast_not_counted_income)
                                            }
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.financialColors.income,
                                            uncheckedThumbColor = Color(0xFF9E9E9E),
                                            uncheckedTrackColor = Color(0xFF2E2E2E),
                                        ),
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_account_expense),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                Switch(
                                    checked = selectedType == TransactionType.Expense,
                                    onCheckedChange = { isExpense ->
                                        selectedType =
                                            if (isExpense) {
                                                TransactionType.Expense
                                            } else {
                                                TransactionType.Income
                                            }
                                        val msg =
                                            if (isExpense) {
                                                context.getString(R.string.detail_toast_counted_spends)
                                            } else {
                                                context.getString(R.string.detail_toast_not_counted_spends)
                                            }
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.financialColors.income,
                                            uncheckedThumbColor = Color(0xFF9E9E9E),
                                            uncheckedTrackColor = Color(0xFF2E2E2E),
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            // Linked Transfer / Card Bill Payment Card
            if (linkedTxn != null) {
                val isRefundLink =
                    selectedType == TransactionType.Refund ||
                        linkedTxn.type == TransactionType.Refund ||
                        TransactionClassifierHelper.isRefundNarrative(linkedTxn.note) ||
                        TransactionClassifierHelper.isRefundNarrative(transaction.note)

                item {
                    val linkedMerchant =
                        linkedTxn.merchantNormalised
                            ?: linkedTxn.merchantRaw
                            ?: stringResource(id = R.string.detail_original_sms)

                    val cardTitle =
                        when {
                            isRefundLink && selectedType == TransactionType.Expense ->
                                "Linked Refund (+${MoneyFormatter.formatMinorUnits(linkedTxn.amount)})"
                            isRefundLink ->
                                "Linked Expense (-${MoneyFormatter.formatMinorUnits(linkedTxn.amount)})"
                            selectedType == TransactionType.Transfer -> {
                                val isCard =
                                    accounts.find { it.id == transaction.accountId }?.type ==
                                        AccountType.CreditCard || transaction.channel == Channel.CARD
                                if (isCard) {
                                    "Card Bill Paid from ${linkedAccount?.name ?: "Bank Account"}"
                                } else {
                                    "Paid to ${linkedAccount?.name ?: "Credit Card"}"
                                }
                            }
                            else -> "Linked Transaction"
                        }

                    val cardSubtitle =
                        when {
                            isRefundLink && selectedType == TransactionType.Expense -> {
                                val netMinor = maxOf(0L, transaction.amount - linkedTxn.amount)
                                "Net Spend: ${MoneyFormatter.formatMinorUnits(netMinor)}"
                            }
                            isRefundLink ->
                                "Refund for spend at $linkedMerchant"
                            else ->
                                "Reconciled Transfer • No double counting"
                        }

                    val iconTint =
                        if (isRefundLink) {
                            MaterialTheme.financialColors.income
                        } else {
                            MaterialTheme.financialColors.transfer
                        }

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (onNavigateToTransaction != null) {
                                        Modifier.clickable { onNavigateToTransaction(linkedTxn.id) }
                                    } else {
                                        Modifier
                                    },
                                ),
                        shape = RoundedCornerShape(20.dp),
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
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = iconTint.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_category_transfer),
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.padding(9.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cardTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cardSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onUnlinkTransaction?.invoke(transaction.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_unlink_button),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            } else {
                val isAcTransfer =
                    selectedCategory == "A/C Transfer" ||
                        selectedCategory == "Transfer" ||
                        selectedType == TransactionType.Transfer
                val isRefund =
                    selectedCategory.equals("Refund", ignoreCase = true) ||
                        selectedType == TransactionType.Refund
                val isExpense = selectedType == TransactionType.Expense && !isAcTransfer

                val canLink = isAcTransfer || isRefund || isExpense

                if (canLink) {
                    item {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showLinkSheet = true },
                            shape = RoundedCornerShape(20.dp),
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
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    val iconTint =
                                        if (isAcTransfer) {
                                            MaterialTheme.financialColors.transfer
                                        } else {
                                            MaterialTheme.financialColors.income
                                        }
                                    val iconRes =
                                        if (isAcTransfer) {
                                            DesignR.drawable.ic_category_transfer
                                        } else {
                                            DesignR.drawable.ic_arrow_outward
                                        }
                                    Surface(
                                        shape = CircleShape,
                                        color = iconTint.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.padding(9.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        val linkPromptTitle =
                                            when {
                                                isAcTransfer && isCreditTxn ->
                                                    stringResource(id = R.string.detail_link_debit_button)
                                                isAcTransfer && !isCreditTxn ->
                                                    stringResource(id = R.string.detail_link_credit_button)
                                                isRefund ->
                                                    stringResource(id = R.string.detail_link_expense_button)
                                                else ->
                                                    stringResource(id = R.string.detail_link_refund_button)
                                            }
                                        val linkPromptSubtitle =
                                            when {
                                                isAcTransfer && isCreditTxn ->
                                                    stringResource(id = R.string.link_sheet_debit_title)
                                                isAcTransfer && !isCreditTxn ->
                                                    stringResource(id = R.string.link_sheet_credit_title)
                                                isRefund ->
                                                    stringResource(id = R.string.link_sheet_refund_title)
                                                else ->
                                                    stringResource(id = R.string.link_sheet_expense_title)
                                            }
                                        Text(
                                            text = linkPromptTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = linkPromptSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Button(
                                    onClick = { showLinkSheet = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (isAcTransfer) {
                                                    MaterialTheme.financialColors.transfer
                                                } else {
                                                    MaterialTheme.financialColors.income
                                                },
                                            contentColor = Color.Black,
                                        ),
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    val buttonText =
                                        when {
                                            isAcTransfer && isCreditTxn ->
                                                stringResource(id = R.string.detail_link_debit_button)
                                            isAcTransfer && !isCreditTxn ->
                                                stringResource(id = R.string.detail_link_credit_button)
                                            isRefund ->
                                                stringResource(id = R.string.detail_link_expense_button)
                                            else ->
                                                stringResource(id = R.string.detail_link_refund_button)
                                        }
                                    Text(
                                        text = buttonText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedType == TransactionType.Transfer) {
                    val targetCard =
                        TransactionClassifierHelper.extractTargetCardLast4(transaction.note ?: "")
                    if (targetCard != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.financialColors.transfer.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = DesignR.drawable.ic_category_transfer),
                                            contentDescription = null,
                                            tint = MaterialTheme.financialColors.transfer,
                                            modifier = Modifier.padding(9.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Payment to Card ••$targetCard",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Card Bill Payment • Excluded from Expense",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Notes Card
            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showEditNoteDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_notes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.financialColors.income,
                            )
                            Text(
                                text = stringResource(id = R.string.detail_section_note),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                        Text(
                            text =
                                if (noteText.isBlank()) {
                                    stringResource(id = R.string.detail_notes_tap_to_add)
                                } else {
                                    noteText
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (noteText.isBlank()) {
                                    Color(0xFF9E9E9E)
                                } else {
                                    Color.White
                                },
                        )
                    }
                }
            }

            // 4. Split & Attach Row (2 side-by-side cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Split Card
                    if (isCreditTxn) {
                        Card(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.financialColors.cardBackground.copy(alpha = 0.5f),
                                ),
                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2E2E2E)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_split),
                                        contentDescription = null,
                                        tint = Color(0xFF9E9E9E),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.detail_split_not_available_credit),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E),
                                    minLines = 3,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showSplitSheet = true },
                            shape = RoundedCornerShape(20.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.financialColors.cardBackground,
                                ),
                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7C3AED)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = DesignR.drawable.ic_split),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    if (isSplitActive) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.financialColors.income.copy(alpha = 0.2f),
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.split_active_pill),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.financialColors.income,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = stringResource(id = R.string.detail_split_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.financialColors.paleLimeAddButton,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text =
                                            if (isSplitActive) {
                                                stringResource(id = R.string.split_edit_action)
                                            } else {
                                                stringResource(id = R.string.detail_split_action)
                                            },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.financialColors.onPaleLimeAddButton,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Attach Card
                    val hasAttachedPhoto = receiptFile != null && receiptThumbnail != null
                    Card(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (hasAttachedPhoto) {
                                        showReceiptViewDialog = true
                                    } else {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                                            ),
                                        )
                                    }
                                },
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.financialColors.cardBackground,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (hasAttachedPhoto && receiptThumbnail != null) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.financialColors.cardBackground),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        bitmap = receiptThumbnail.asImageBitmap(),
                                        contentDescription = stringResource(id = R.string.receipt_thumbnail_desc),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.financialColors.income,
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.financialColors.onIncome,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = stringResource(id = R.string.receipt_attached_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.financialColors.onIncome,
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.financialColors.paleLimeAddButton,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.receipt_view_action),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.financialColors.onPaleLimeAddButton,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                }
                            } else {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.financialColors.cardBackground),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_receipt_photo),
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.income,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.detail_attach_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.financialColors.paleLimeAddButton,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.detail_attach_action),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.financialColors.onPaleLimeAddButton,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Tags Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_label),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.financialColors.income,
                                )
                                Text(
                                    text = stringResource(id = R.string.detail_tags_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                            IconButton(
                                onClick = { showAddTagDialog = true },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.financialColors.income,
                                )
                            }
                        }

                        if (activeTags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                activeTags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.financialColors.cardBackground,
                                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = tag,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(
                                                    id = R.string.tags_remove_tag,
                                                    tag,
                                                ),
                                                tint = Color(0xFF9E9E9E),
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable {
                                                        noteText = TagParser.removeTag(noteText, tag)
                                                    },
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(id = R.string.tags_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E),
                            )
                        }
                    }
                }
            }

            // 6. Original Full SMS Message Card (Exact verbatim message)
            val rawSms = transaction.note?.takeIf { it.isNotBlank() }
            if (!rawSms.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.financialColors.cardBackground,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_notes),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.financialColors.income,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.detail_original_sms),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                                val copiedMsg = stringResource(id = R.string.detail_copied)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(rawSms))
                                        scope.launch { snackbarHostState.showSnackbar(copiedMsg) }
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_copy),
                                        contentDescription = stringResource(id = R.string.detail_copy_ref),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.financialColors.income,
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.financialColors.cardBackground,
                                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = rawSms,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(14.dp),
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3,
                                )
                            }
                        }
                    }
                }
            }

            // 7. Other Info Card (Location, Ref No)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_notes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.financialColors.income,
                            )
                            Text(
                                text = stringResource(id = R.string.detail_other_info_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }

                        // Location Row (only displayed when location is present)
                        val txnLocation =
                            remember(transaction.latitude, transaction.longitude, transaction.locationLabel, rawSms, noteText, merchantName) {
                                LocationHelper.extractLocation(
                                    latitude = transaction.latitude,
                                    longitude = transaction.longitude,
                                    locationLabel = transaction.locationLabel,
                                    rawSms = rawSms,
                                    note = noteText,
                                    merchantName = merchantName,
                                )
                            }

                        if (txnLocation != null) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            LocationHelper.openInGoogleMaps(context, txnLocation)
                                        }
                                        .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.detail_location_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = txnLocation.displayLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.detail_location_open_maps),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.financialColors.income,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier =
                                        Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                MaterialTheme.financialColors.income.copy(alpha = 0.15f),
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_location),
                                        contentDescription = stringResource(id = R.string.detail_location_open_maps),
                                        tint = MaterialTheme.financialColors.income,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }

                        // Ref No Row
                        val refNo = transaction.refNumber ?: "N/A"
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
                                    text = refNo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            val copiedMsg = stringResource(id = R.string.detail_copied)
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(refNo))
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
            containerColor = MaterialTheme.financialColors.cardBackground,
        ) {
            CategoryPickerSheetContent(
                selectedCategory = selectedCategory,
                isCredit = isCreditTxn,
                onCategorySelected = { cat ->
                    selectedCategory = cat
                    if (cat in INWARD_INCOME_CATEGORIES) {
                        selectedType = TransactionType.Income
                    } else if (cat == "Refund") {
                        selectedType = TransactionType.Refund
                    } else if (cat in listOf("Transfer", "A/C Transfer", "Bill Payment", "Investment")) {
                        selectedType = TransactionType.Transfer
                    } else if (cat in EXPENSE_CATEGORIES) {
                        selectedType = TransactionType.Expense
                    }
                    showCategorySheet = false
                },
                onDismiss = { showCategorySheet = false },
            )
        }
    }

    // Merchant Rule Propagation & Similar Transactions Sheet
    if (showPropagateDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val zoneId = ZoneId.systemDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

        ModalBottomSheet(
            onDismissRequest = { showPropagateDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                // Header text matching screenshot
                Text(
                    text =
                        stringResource(
                            id = R.string.propagate_sheet_subtitle,
                            similarTransactions.size,
                            merchantName,
                            selectedCategory,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grouped Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        similarTransactions.forEachIndexed { index, other ->
                            val isChecked = checkedSimilarIds.contains(other.id)
                            val otherCat = other.categorySource ?: "Other"
                            val dateStr =
                                Instant.ofEpochMilli(other.dateTime).atZone(zoneId).format(dateFormatter)
                            val amountStr = MoneyFormatter.formatMinorUnits(other.amount)

                            val otherNote = other.note
                            val otherTag =
                                when {
                                    otherNote != null && otherNote.contains("#") -> {
                                        otherNote.split("\\s+".toRegex())
                                            .firstOrNull { it.startsWith("#") } ?: "#Online"
                                    }
                                    other.channel in listOf(
                                        Channel.UPI,
                                        Channel.NEFT,
                                        Channel.IMPS,
                                        Channel.CARD,
                                    ) -> "#Online"
                                    else -> "#Offline"
                                }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            checkedSimilarIds =
                                                if (isChecked) {
                                                    checkedSimilarIds - other.id
                                                } else {
                                                    checkedSimilarIds + other.id
                                                }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        checkedSimilarIds =
                                            if (checked) {
                                                checkedSimilarIds + other.id
                                            } else {
                                                checkedSimilarIds - other.id
                                            }
                                    },
                                    colors =
                                        CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF7C4DFF),
                                            checkmarkColor = Color.White,
                                        ),
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                CategoryBadge(categoryName = otherCat, size = 36.dp)

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = merchantName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF2C3248))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = otherTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = Color(0xFF8CA3D8),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = amountStr,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (index < similarTransactions.size - 1) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Actions: ✕ Cancel & ✓ Change
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { showPropagateDialog = false },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.income,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = android.R.string.cancel),
                            color = MaterialTheme.financialColors.income,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Button(
                        onClick = {
                            showPropagateDialog = false
                            val updated =
                                transaction.copy(
                                    type = selectedType,
                                    categorySource = selectedCategory,
                                    note = noteText.takeIf { it.isNotBlank() },
                                    isReviewed = true,
                                )
                            val updatedSimilar =
                                similarTransactions
                                    .filter { checkedSimilarIds.contains(it.id) }
                                    .map { it.copy(categorySource = selectedCategory, isReviewed = true) }

                            if (onSaveTransactions != null) {
                                onSaveTransactions(listOf(updated) + updatedSimilar, true)
                            } else {
                                onSave(updated, true)
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.income,
                                contentColor = MaterialTheme.financialColors.onIncome,
                            ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.propagate_change_button),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // Edit Note Dialog
    if (showEditNoteDialog) {
        var tempNote by remember { mutableStateOf(noteText) }
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text(text = stringResource(id = R.string.detail_section_note)) },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    placeholder = { Text(text = stringResource(id = R.string.detail_note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        noteText = tempNote
                        showEditNoteDialog = false
                    },
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNoteDialog = false }) {
                    Text(text = stringResource(id = R.string.detail_delete_cancel))
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

    // Split Expense Sheet
    if (showSplitSheet) {
        SplitExpenseSheet(
            totalAmountMinor = transaction.amount,
            merchantName = merchantName,
            onDismiss = { showSplitSheet = false },
            onSaveSplit = { summary, _ ->
                noteText = SplitExpenseCalculator.appendOrUpdateSplit(noteText, summary)
            },
        )
    }

    // Receipt View Dialog
    if (showReceiptViewDialog) {
        ReceiptViewDialog(
            bitmap = receiptFullBitmap,
            onDismiss = { showReceiptViewDialog = false },
            onChangePhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemovePhoto = {
                ReceiptAttachmentHelper.removeReceipt(context, transaction.id)
                receiptFile = null
            },
        )
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AddTagDialog(
            currentTags = activeTags,
            onAddTag = { tag ->
                noteText = TagParser.addTag(noteText, tag)
            },
            onRemoveTag = { tag ->
                noteText = TagParser.removeTag(noteText, tag)
            },
            onDismiss = { showAddTagDialog = false },
        )
    }

    // Link Transaction Sheet (Refund / Expense / Transfer)
    if (showLinkSheet) {
        val isTransfer =
            selectedCategory == "A/C Transfer" ||
                selectedCategory == "Transfer" ||
                selectedType == TransactionType.Transfer
        LinkTransactionSheet(
            targetTransaction = transaction,
            allTransactions = allTransactions,
            isTransferLink = isTransfer,
            isCreditTarget = isCreditTxn,
            onLink = { candidate ->
                val isRefund =
                    !isTransfer && (selectedType == TransactionType.Refund ||
                        candidate.type == TransactionType.Refund ||
                        selectedCategory.equals("Refund", ignoreCase = true) ||
                        candidate.categorySource.equals("Refund", ignoreCase = true) ||
                        TransactionClassifierHelper.isRefundNarrative(candidate.note) ||
                        TransactionClassifierHelper.isRefundNarrative(transaction.note))
                onLinkTransactions?.invoke(transaction.id, candidate.id, isRefund)
                showLinkSheet = false
            },
            onDismiss = { showLinkSheet = false },
        )
    }
}

internal val EXPENSE_CATEGORIES =
    listOf(
        "Bills",
        "Donation",
        "EMI",
        "Entertainment",
        "Food & Drinks",
        "Fuel",
        "Groceries",
        "Health",
        "Investment",
        "Other",
        "Shopping",
        "Transfer",
        "Travel",
        "Shared",
    )

internal val INWARD_INCOME_CATEGORIES =
    listOf(
        "Bank Deposit",
        "Business",
        "Interest",
        "Salary",
    )

internal val INWARD_NON_INCOME_CATEGORIES =
    listOf(
        "A/C Transfer",
        "Bill Payment",
        "Credit",
        "Investment",
        "Loan",
        "Recharge",
        "Refund",
        "Reimbursement",
        "Rewards",
    )

@Composable
internal fun CategoryPickerSheetContent(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isCredit: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var newCategoryText by remember { mutableStateOf("") }
    var currentSelected by remember { mutableStateOf(selectedCategory) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.detail_category_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            if (isCredit) {
                CategorySectionHeader(
                    title = stringResource(id = R.string.category_section_mark_as_income),
                )
                Spacer(modifier = Modifier.height(14.dp))
                CategoryGrid(
                    categories = INWARD_INCOME_CATEGORIES,
                    selectedCategory = currentSelected,
                    onCategorySelected = { cat ->
                        currentSelected = cat
                        onCategorySelected(cat)
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                CategorySectionHeader(
                    title = stringResource(id = R.string.category_section_mark_as_not_income),
                )
                Spacer(modifier = Modifier.height(14.dp))
                CategoryGrid(
                    categories = INWARD_NON_INCOME_CATEGORIES,
                    selectedCategory = currentSelected,
                    onCategorySelected = { cat ->
                        currentSelected = cat
                        onCategorySelected(cat)
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Footer explanation card for selected category
                val isSelectedIncome = currentSelected in INWARD_INCOME_CATEGORIES
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text =
                                    if (isSelectedIncome) {
                                        stringResource(id = R.string.detail_toast_counted_income)
                                    } else {
                                        stringResource(id = R.string.detail_toast_not_counted_income)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.category_mark_as_income_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E),
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isSelectedIncome,
                            onCheckedChange = { makeIncome ->
                                val target =
                                    if (makeIncome) {
                                        INWARD_INCOME_CATEGORIES.first()
                                    } else {
                                        INWARD_NON_INCOME_CATEGORIES.first()
                                    }
                                currentSelected = target
                                onCategorySelected(target)
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.financialColors.income,
                                    uncheckedThumbColor = Color(0xFF9E9E9E),
                                    uncheckedTrackColor = Color(0xFF2E2E2E),
                                ),
                        )
                    }
                }
            } else {
                CategoryGrid(
                    categories = EXPENSE_CATEGORIES,
                    selectedCategory = currentSelected,
                    onCategorySelected = { cat ->
                        currentSelected = cat
                        onCategorySelected(cat)
                    },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.financialColors.cardBackground,
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .background(
                                    color =
                                        if (isCredit) {
                                            MaterialTheme.financialColors.income
                                        } else {
                                            MaterialTheme.financialColors.expense
                                        },
                                    shape = CircleShape,
                                ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.category_new_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9E9E9E),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                    )
                    if (newCategoryText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val trimmed = newCategoryText.trim()
                                if (trimmed.isNotBlank()) {
                                    currentSelected = trimmed
                                    onCategorySelected(trimmed)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.income,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.financialColors.hairlineDivider,
        )
        Text(
            text = "  $title  ",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9E9E9E),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.financialColors.hairlineDivider,
        )
    }
}

@Composable
private fun CategoryGrid(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chunked = categories.chunked(4)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (cat in row) {
                    CategoryGridItem(
                        category = cat,
                        isSelected = cat.equals(selectedCategory, ignoreCase = true),
                        onClick = { onCategorySelected(cat) },
                        modifier = Modifier.weight(1f),
                    )
                }
                for (i in 0 until (4 - row.size)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryGridItem(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconRes = CategoryIconHelper.getCategoryIcon(category)
    val colorToken = CategoryColors.forCategory(category)
    val incomeGreen = MaterialTheme.financialColors.income

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .background(
                        color =
                            if (isSelected) {
                                incomeGreen.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.financialColors.cardBackground
                            },
                        shape = CircleShape,
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, incomeGreen, CircleShape)
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.financialColors.hairlineDivider,
                                CircleShape,
                            )
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = category,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) incomeGreen else colorToken.color,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = if (isSelected) incomeGreen else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
