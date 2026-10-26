package com.kharcha.feature.transactions.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    var propagateToAllMerchants by remember { mutableStateOf(true) }

    val merchantName =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: stringResource(id = R.string.detail_original_sms)

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.txn_detail_close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
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
                        containerColor = MaterialTheme.colorScheme.surface,
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

            // 1. Hero Card (Matching Fold Money screenshot 1)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
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
                                textDecoration = TextDecoration.Underline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
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
                                    )
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_bar_chart),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                                contentDescription = null,
                                tint =
                                    if (selectedType == TransactionType.Income) {
                                        MaterialTheme.financialColors.income
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = displayAmount,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clickable Category Pill: [ 🛒 Groceries ]
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Date and Time
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 2. Account & Expense Toggle Card
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                )
                                Text(
                                    text = accountTypeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_account_expense),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    },
                                    colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.financialColors.income,
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            // Linked Transfer / Card Bill Payment Card
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

            val isRefundLink =
                linkedTxn != null &&
                    (selectedType == TransactionType.Refund ||
                        linkedTxn.type == TransactionType.Refund ||
                        (linkedTxn.note != null &&
                            TransactionClassifierHelper.isRefundNarrative(linkedTxn.note)) ||
                        (transaction.note != null &&
                            TransactionClassifierHelper.isRefundNarrative(transaction.note)))

            if (linkedTxn != null) {
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
                val isExpense = selectedType == TransactionType.Expense
                val isRefundOrIncome =
                    selectedType == TransactionType.Refund ||
                        selectedType == TransactionType.Income ||
                        (transaction.note != null &&
                            TransactionClassifierHelper.isRefundNarrative(transaction.note))

                if (isExpense || isRefundOrIncome) {
                    item {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showLinkSheet = true },
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.financialColors.income.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                                            contentDescription = null,
                                            tint = MaterialTheme.financialColors.income,
                                            modifier = Modifier.padding(9.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        val linkPromptTitle =
                                            if (isExpense) {
                                                stringResource(id = R.string.detail_link_refund_button)
                                            } else {
                                                stringResource(id = R.string.detail_link_expense_button)
                                            }
                                        val linkPromptSubtitle =
                                            if (isExpense) {
                                                "Offset this expense with an incoming refund"
                                            } else {
                                                "Link this refund to the original spend"
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(
                                        text = "Link",
                                        style = MaterialTheme.typography.labelMedium,
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
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showEditNoteDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(id = R.string.detail_section_note),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
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
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
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
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showSplitSheet = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
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
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_split),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                if (isSplitActive) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.split_active_pill),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = stringResource(id = R.string.detail_split_title),
                                style = MaterialTheme.typography.bodySmall,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = if (isSplitActive) {
                                        stringResource(id = R.string.split_edit_action)
                                    } else {
                                        stringResource(id = R.string.detail_split_action)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                )
                            }
                        }
                    }

                    // Attach Card
                    val hasAttachedPhoto = receiptFile != null && receiptThumbnail != null
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
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
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (hasAttachedPhoto && receiptThumbnail != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                        modifier = Modifier
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
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.receipt_view_action),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_receipt_photo),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.detail_attach_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.detail_attach_action),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
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
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(id = R.string.detail_tags_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
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
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = tag,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(
                                                    id = R.string.tags_remove_tag,
                                                    tag,
                                                ),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        shape = RoundedCornerShape(18.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
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
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.detail_original_sms),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = rawSms,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(id = R.string.detail_other_info_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Location Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.detail_location_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(id = R.string.detail_location_action),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_location),
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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

    // Link Transaction Sheet (Refund / Expense)
    if (showLinkSheet) {
        LinkTransactionSheet(
            targetTransaction = transaction,
            allTransactions = allTransactions,
            onLink = { candidate ->
                val isRefund =
                    selectedType == TransactionType.Refund ||
                        candidate.type == TransactionType.Refund ||
                        (candidate.note != null &&
                            TransactionClassifierHelper.isRefundNarrative(candidate.note)) ||
                        (transaction.note != null &&
                            TransactionClassifierHelper.isRefundNarrative(transaction.note))
                onLinkTransactions?.invoke(transaction.id, candidate.id, isRefund)
                showLinkSheet = false
            },
            onDismiss = { showLinkSheet = false },
        )
    }
}

@Composable
internal fun CategoryPickerSheetContent(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onDismiss: () -> Unit,
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

    Column(
        modifier =
            modifier
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCategorySelected(category) },
                    shape = RoundedCornerShape(12.dp),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                ) {
                    Row(
                        modifier =
                            Modifier
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
                                color =
                                    if (isSelected) {
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
