package com.kharcha.feature.transactions.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.feature.transactions.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseSheet(
    totalAmountMinor: Long,
    merchantName: String,
    onDismiss: () -> Unit,
    onSaveSplit: (summary: String, shares: List<ParticipantShare>) -> Unit,
    modifier: Modifier = Modifier,
    initialCount: Int = SplitExpenseCalculator.MIN_PARTICIPANTS,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val defaultYou = stringResource(id = R.string.split_default_you)

    var participantCount by remember {
        mutableIntStateOf(
            initialCount.coerceIn(
                SplitExpenseCalculator.MIN_PARTICIPANTS,
                SplitExpenseCalculator.MAX_PARTICIPANTS,
            ),
        )
    }

    val participantNames = remember {
        mutableStateListOf<String>().apply {
            add(defaultYou)
            for (i in 2..participantCount) {
                add("Person $i")
            }
        }
    }

    fun syncParticipants(newCount: Int) {
        val targetCount = newCount.coerceIn(
            SplitExpenseCalculator.MIN_PARTICIPANTS,
            SplitExpenseCalculator.MAX_PARTICIPANTS,
        )
        participantCount = targetCount
        while (participantNames.size < targetCount) {
            val nextIndex = participantNames.size + 1
            participantNames.add("Person $nextIndex")
        }
        while (participantNames.size > targetCount) {
            participantNames.removeAt(participantNames.lastIndex)
        }
    }

    val shares = remember(totalAmountMinor, participantNames.toList()) {
        SplitExpenseCalculator.calculateShares(
            totalAmountMinor = totalAmountMinor,
            participantNames = participantNames.toList(),
        )
    }

    val sharePerPerson = shares.firstOrNull()?.amountMinor ?: 0L
    val formattedTotal = MoneyFormatter.formatMinorUnits(totalAmountMinor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Title & Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.split_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.split_merchant, merchantName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Participant Count Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.split_participants_count, participantCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.split_per_person,
                            MoneyFormatter.formatMinorUnits(sharePerPerson),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { syncParticipants(participantCount - 1) },
                        enabled = participantCount > SplitExpenseCalculator.MIN_PARTICIPANTS,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (participantCount > SplitExpenseCalculator.MIN_PARTICIPANTS) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_category_bills),
                                contentDescription = stringResource(id = R.string.split_decrease_participants),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = participantCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    IconButton(
                        onClick = { syncParticipants(participantCount + 1) },
                        enabled = participantCount < SplitExpenseCalculator.MAX_PARTICIPANTS,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (participantCount < SplitExpenseCalculator.MAX_PARTICIPANTS) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.split_increase_participants),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Named Participants List with Editable Names & Shares
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(participantNames) { index, name ->
                    val share = shares.getOrNull(index)?.amountMinor ?: 0L
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                participantNames[index] = newName
                            },
                            singleLine = true,
                            placeholder = {
                                Text(text = stringResource(id = R.string.split_participant_name_hint))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = MoneyFormatter.formatMinorUnits(share),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action: Share via WhatsApp / SMS
            OutlinedButton(
                onClick = {
                    val shareAmount = if (shares.size > 1) shares[1].amountMinor else shares.first().amountMinor
                    val message = SplitExpenseCalculator.formatShareMessage(
                        merchant = merchantName,
                        totalAmountMinor = totalAmountMinor,
                        shareAmountMinor = shareAmount,
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    val chooser = Intent.createChooser(
                        sendIntent,
                        context.getString(R.string.split_share_chooser_title),
                    )
                    context.startActivity(chooser)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.split_share_action),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Action: Save Split
            Button(
                onClick = {
                    val summary = SplitExpenseCalculator.formatSplitSummary(shares)
                    onSaveSplit(summary, shares)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.financialColors.income,
                    contentColor = MaterialTheme.financialColors.onIncome,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.split_save_action),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
