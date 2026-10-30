@file:Suppress("TooManyFunctions")

package com.kharcha.feature.transactions.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.component.KharchaPillSnackbar
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.feature.transactions.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

// ---------------------------------------------------------------------------
// Data models (private to this file)
// ---------------------------------------------------------------------------

private data class SenderGroup(
    val brandName: String,
    val messages: List<RawSmsUi>,
) {
    val latestMessage: RawSmsUi get() = messages.first()
    val rawSenderIds: Set<String> get() = messages.map { it.rawSender }.toSet()
}

private data class RawSmsUi(
    val id: Long,
    val rawSender: String,
    val body: String,
    val receivedAt: Long,
)

private enum class SupportStatus { SUPPORTED, REQUEST_TO_SUPPORT, REPORTED }

// ---------------------------------------------------------------------------
// Sender logo colour palette (deterministic per-brand)
// ---------------------------------------------------------------------------

private val senderLogoPalette = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
    Color(0xFFC62828),
    Color(0xFF00695C),
    Color(0xFFE65100),
    Color(0xFF4527A0),
    Color(0xFF00838F),
)

// ---------------------------------------------------------------------------
// Known bank brand mappings
// ---------------------------------------------------------------------------

private val brandMap: Map<String, String> = mapOf(
    "HDFCBN" to "HDFC",
    "HDFCBK" to "HDFC",
    "HDFC" to "HDFC",
    "ICICIB" to "ICICI",
    "ICICI" to "ICICI",
    "SBIINB" to "SBI",
    "SBIIN" to "SBI",
    "SBI" to "SBI",
    "IDFCFB" to "IDFC FIRST",
    "IDFCB" to "IDFC FIRST",
    "IDFC" to "IDFC FIRST",
    "AXISBK" to "Axis Bank",
    "AXIS" to "Axis Bank",
    "KOTAKB" to "Kotak",
    "KOTAK" to "Kotak",
    "PAYTMB" to "Paytm",
    "PAYTM" to "Paytm",
    "YESBNK" to "Yes Bank",
    "YES" to "Yes Bank",
    "BOBIBN" to "Bank of Baroda",
    "BOB" to "Bank of Baroda",
    "PNBSMS" to "PNB",
    "PNB" to "PNB",
    "CBSSBI" to "SBI",
    "CANBNK" to "Canara Bank",
    "UNIONB" to "Union Bank",
    "INDBNK" to "Indian Bank",
    "CENTBK" to "Central Bank",
    "BOIIND" to "BOI",
)

private val prefixRegex = Regex("^[A-Z]{2}-")
private val suffixRegex = Regex("-[A-Z]$")

/**
 * Normalise a raw SMS sender id into a human-friendly brand name.
 *
 * Example: "JM-HDFCBN-S" → "HDFC", "VM-HDFCBK-S" → "HDFC", "+919876543210" → "+919876543210".
 */
private fun normalizeSender(raw: String): String {
    val upper = raw.trim().uppercase(Locale.ROOT)

    // Phone numbers stay as-is
    if (upper.any { it.isDigit() } && upper.count { it.isLetter() } <= 2) {
        return raw.trim()
    }

    // Strip 2-letter carrier prefix ("JM-", "VM-", "AD-" etc.)
    var token = prefixRegex.replace(upper, "")
    // Strip trailing route code ("-S", "-T", "-P", "-G", "-N")
    token = suffixRegex.replace(token, "")

    return brandMap[token] ?: token
}

// ---------------------------------------------------------------------------
// Time formatting helpers
// ---------------------------------------------------------------------------

private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ROOT)
private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT)

/** Level 1 card time: today → "HH:mm", else → "d MMM". */
private fun formatCardTime(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val msgDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val now = LocalDate.now(zone)
    return if (msgDate == now) {
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime().format(shortTimeFormatter)
    } else {
        msgDate.format(shortDateFormatter)
    }
}

/** Level 2 meta: "Today, 3:55 pm" / "Yesterday, 7:03 pm" / "28 Oct, 9:20 pm". */
private fun formatRelativeDateTime(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(timestamp).atZone(zone)
    val msgDate = zdt.toLocalDate()
    val timeStr = zdt.toLocalTime().format(clockFormatter).lowercase(Locale.ROOT)
    val now = LocalDate.now(zone)
    return when (msgDate) {
        now -> "Today, $timeStr"
        now.minusDays(1) -> "Yesterday, $timeStr"
        else -> "${msgDate.format(shortDateFormatter)}, $timeStr"
    }
}

// ---------------------------------------------------------------------------
// Public entry-point (same signature as old screen)
// ---------------------------------------------------------------------------

/**
 * Report Undetected SMS — two-level sender / thread flow (UI Spec Section 9).
 */
@Composable
fun ReportUndetectedSmsScreen(
    onSubmitReport: (sender: String, rawSms: String, maskDetails: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ---- SMS inbox loading ----
    val context = LocalContext.current
    var allMessages by remember { mutableStateOf<List<RawSmsUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var permissionDenied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            @Suppress("TooGenericExceptionCaught")
            try {
                val result = mutableListOf<RawSmsUi>()
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date")
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "date DESC",
                )
                cursor?.use { c ->
                    val idIdx = c.getColumnIndex("_id")
                    val addrIdx = c.getColumnIndex("address")
                    val bodyIdx = c.getColumnIndex("body")
                    val dateIdx = c.getColumnIndex("date")
                    while (c.moveToNext()) {
                        result.add(
                            RawSmsUi(
                                id = if (idIdx != -1) c.getLong(idIdx) else 0L,
                                rawSender = if (addrIdx != -1) {
                                    c.getString(addrIdx).orEmpty()
                                } else {
                                    ""
                                },
                                body = if (bodyIdx != -1) {
                                    c.getString(bodyIdx).orEmpty()
                                } else {
                                    ""
                                },
                                receivedAt = if (dateIdx != -1) c.getLong(dateIdx) else 0L,
                            ),
                        )
                    }
                }
                allMessages = result
            } catch (_: SecurityException) {
                permissionDenied = true
            } catch (_: Exception) {
                // best-effort — return whatever was collected
            }
            isLoading = false
        }
    }

    // ---- Group by brand ----
    val senderGroups = remember(allMessages) {
        allMessages
            .groupBy { normalizeSender(it.rawSender) }
            .map { (brand, msgs) -> SenderGroup(brandName = brand, messages = msgs) }
            .sortedByDescending { it.latestMessage.receivedAt }
    }

    // ---- Two-level navigation ----
    var selectedSender by remember { mutableStateOf<SenderGroup?>(null) }

    if (selectedSender != null) {
        BackHandler { selectedSender = null }
        SenderThreadContent(
            group = selectedSender!!,
            onSubmitReport = onSubmitReport,
            onBack = { selectedSender = null },
            modifier = modifier,
        )
    } else {
        BackHandler(onBack = onDismiss)
        SenderListContent(
            groups = senderGroups,
            isLoading = isLoading,
            permissionDenied = permissionDenied,
            onSelectSender = { selectedSender = it },
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

// ---------------------------------------------------------------------------
// Level 1 — Sender list
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SenderListContent(
    groups: List<SenderGroup>,
    isLoading: Boolean,
    permissionDenied: Boolean,
    onSelectSender: (SenderGroup) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) {
            groups
        } else {
            val q = searchQuery.trim()
            groups.filter { g ->
                g.brandName.contains(q, ignoreCase = true) ||
                    g.messages.any { it.body.contains(q, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.report_sms_sender_list_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.report_sms_back_desc),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(
                                R.string.report_sms_search_icon_desc,
                            ),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(
                                R.string.report_sms_info_icon_desc,
                            ),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Animated search bar
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    hint = stringResource(R.string.report_sms_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.financialColors.limeAccent,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.report_sms_loading),
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                    }
                }
                permissionDenied -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.report_sms_permission_needed),
                            color = MaterialTheme.financialColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }
                filteredGroups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.report_sms_no_senders),
                            color = MaterialTheme.financialColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = filteredGroups,
                            key = { it.brandName },
                        ) { group ->
                            SenderGroupCard(
                                group = group,
                                onClick = { onSelectSender(group) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(
                        text = "OK",
                        color = MaterialTheme.financialColors.limeAccent,
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.report_sms_info_title),
                    color = MaterialTheme.financialColors.textPrimary,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.report_sms_info_body),
                    color = MaterialTheme.financialColors.textSecondary,
                )
            },
            containerColor = MaterialTheme.financialColors.cardBackground,
        )
    }
}

// ---------------------------------------------------------------------------
// Level 2 — Sender thread
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SenderThreadContent(
    group: SenderGroup,
    onSubmitReport: (sender: String, rawSms: String, maskDetails: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val reportedIds = remember { mutableStateMapOf<Long, Boolean>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val reportingMsg = stringResource(R.string.report_sms_reporting_snackbar)

    val filteredMessages = remember(group.messages, searchQuery) {
        if (searchQuery.isBlank()) {
            group.messages
        } else {
            val q = searchQuery.trim()
            group.messages.filter { it.body.contains(q, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                KharchaPillSnackbar(snackbarData = data)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SenderLogoTile(
                            brand = group.brandName,
                            size = 28.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = group.brandName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.report_sms_back_desc),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(
                                R.string.report_sms_search_icon_desc,
                            ),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    hint = stringResource(R.string.report_sms_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.report_sms_thread_empty),
                        color = MaterialTheme.financialColors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                ) {
                    LazyColumn {
                        itemsIndexed(
                            items = filteredMessages,
                            key = { _, msg -> msg.id },
                        ) { index, sms ->
                            val status = when {
                                reportedIds[sms.id] == true -> SupportStatus.REPORTED
                                else -> SupportStatus.REQUEST_TO_SUPPORT
                            }

                            SmsMessageBlock(
                                sms = sms,
                                status = status,
                                onRequestSupport = {
                                    reportedIds[sms.id] = true
                                    onSubmitReport(sms.rawSender, sms.body, false)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(reportingMsg)
                                    }
                                },
                            )

                            if (index < filteredMessages.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.financialColors.hairlineDivider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Composable building blocks
// ---------------------------------------------------------------------------

@Composable
private fun SenderGroupCard(
    group: SenderGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.financialColors.cardBackground,
        ),
    ) {
        // Header strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.financialColors.raisedRowBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SenderLogoTile(brand = group.brandName, size = 24.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = group.brandName,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.financialColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatCardTime(group.latestMessage.receivedAt),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.financialColors.textSecondary,
            )
        }

        // Body preview
        Text(
            text = group.latestMessage.body,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.financialColors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SenderLogoTile(
    brand: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val color = remember(brand) {
        val idx = brand.hashCode().absoluteValue % senderLogoPalette.size
        senderLogoPalette[idx]
    }
    val initials = remember(brand) {
        brand.take(2).uppercase(Locale.ROOT)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SmsMessageBlock(
    sms: RawSmsUi,
    status: SupportStatus,
    onRequestSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Meta line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = formatRelativeDateTime(sms.receivedAt),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.financialColors.textSecondary,
            )
            MetaDot()
            Text(
                text = sms.rawSender,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.financialColors.textSecondary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MetaDot()
            SupportStatusLabel(
                status = status,
                onRequestSupport = onRequestSupport,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Full SMS body
        Text(
            text = sms.body,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.financialColors.textPrimary,
        )
    }
}

@Composable
private fun MetaDot(modifier: Modifier = Modifier) {
    Text(
        text = " · ",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = MaterialTheme.financialColors.textSecondary,
        modifier = modifier,
    )
}

@Composable
private fun SupportStatusLabel(
    status: SupportStatus,
    onRequestSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        SupportStatus.SUPPORTED -> {
            Text(
                text = stringResource(R.string.report_sms_status_supported),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.financialColors.textSecondary,
                modifier = modifier,
            )
        }
        SupportStatus.REQUEST_TO_SUPPORT -> {
            Text(
                text = stringResource(R.string.report_sms_status_request),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.financialColors.expense,
                fontWeight = FontWeight.Medium,
                modifier = modifier.clickable(onClick = onRequestSupport),
            )
        }
        SupportStatus.REPORTED -> {
            Text(
                text = stringResource(R.string.report_sms_status_reported),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.financialColors.textSecondary,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.financialColors.cardBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (query.isEmpty()) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.financialColors.textSecondary.copy(alpha = 0.6f),
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.financialColors.textPrimary,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.financialColors.limeAccent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
