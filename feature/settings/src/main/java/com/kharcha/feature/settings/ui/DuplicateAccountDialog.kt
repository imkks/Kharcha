package com.kharcha.feature.settings.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.feature.settings.R

@Composable
fun DuplicateAccountDialog(
    source: Account,
    target: Account,
    onMerge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sourceLabel = "${source.bankName ?: source.name} ••${source.last4 ?: ""}"
    val targetLabel = "${target.bankName ?: target.name} ••${target.last4 ?: ""}"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.financialColors.cardBackground,
        title = {
            Text(
                text = stringResource(id = R.string.duplicate_accounts_title),
                color = MaterialTheme.financialColors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(
                    id = R.string.duplicate_accounts_message,
                    sourceLabel,
                    targetLabel,
                ),
                color = MaterialTheme.financialColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onMerge) {
                Text(
                    text = stringResource(id = R.string.duplicate_accounts_merge),
                    color = MaterialTheme.financialColors.limeAccent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.duplicate_accounts_keep),
                    color = MaterialTheme.financialColors.textSecondary,
                )
            }
        },
    )
}
