package com.kharcha.feature.transactions.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kharcha.core.common.CsvExportHelper
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.feature.transactions.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStatementSheet(
    monthTransactions: List<Transaction>,
    allTransactions: List<Transaction>,
    filteredTransactions: List<Transaction>,
    isFilterActive: Boolean,
    selectedMonthLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.financialColors.cardBackground,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.export_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.export_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Current Month
            ExportOptionCard(
                iconRes = DesignR.drawable.ic_calendar,
                title = stringResource(id = R.string.export_current_month_title, selectedMonthLabel),
                subtitle = stringResource(id = R.string.accounts_txn_count, monthTransactions.size),
                onClick = {
                    val csv = CsvExportHelper.generateTransactionsCsv(monthTransactions)
                    val safeMonth = selectedMonthLabel.replace(" ", "_")
                    val fileName = "kharcha_statement_${safeMonth}.csv"
                    shareCsvFile(context, csv, fileName)
                    onDismiss()
                },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Filtered Transactions (if filter active)
            if (isFilterActive) {
                ExportOptionCard(
                    iconRes = DesignR.drawable.ic_label,
                    title = stringResource(id = R.string.export_filtered_title),
                    subtitle = stringResource(id = R.string.accounts_txn_count, filteredTransactions.size),
                    onClick = {
                        val csv = CsvExportHelper.generateTransactionsCsv(filteredTransactions)
                        val fileName = "kharcha_filtered_${System.currentTimeMillis()}.csv"
                        shareCsvFile(context, csv, fileName)
                        onDismiss()
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Option 3: All Transactions
            ExportOptionCard(
                iconRes = DesignR.drawable.ic_arrow_outward,
                title = stringResource(id = R.string.export_all_title),
                subtitle = stringResource(id = R.string.accounts_txn_count, allTransactions.size),
                onClick = {
                    val csv = CsvExportHelper.generateTransactionsCsv(allTransactions)
                    val fileName = "kharcha_statement_all_${System.currentTimeMillis()}.csv"
                    shareCsvFile(context, csv, fileName)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ExportOptionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.financialColors.cardBackground,
            ),
        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.income,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                    )
                }
            }
            Icon(
                painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                contentDescription = null,
                tint = MaterialTheme.financialColors.income,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun shareCsvFile(
    context: Context,
    content: String,
    fileName: String,
) {
    try {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        file.writeText(content)

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, "Export $fileName"))
    } catch (_: Exception) {
        val fallbackIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
            }
        context.startActivity(Intent.createChooser(fallbackIntent, "Export $fileName"))
    }
}
