package com.kharcha.core.designsystem.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Bottom sheet tag manager.
 * Displays existing tags as outlined chips with toggle, and an inline '# New tag' input to add new tags.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagSheet(
    availableTags: List<String>,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onAddNewTag: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newTagInput by remember { mutableStateOf("") }

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
            // Header: Close 'X' - Title - 'Done' (Lime text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.txn_detail_close),
                        tint = Color.White,
                    )
                }

                Text(
                    text = stringResource(id = R.string.tag_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.tag_sheet_done),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.income,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FlowRow of existing tag chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (tag in availableTags) {
                    val isSelected = selectedTags.contains(tag)
                    val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
                    val border =
                        if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.financialColors.income)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider)
                        }
                    val bg =
                        if (isSelected) {
                            MaterialTheme.financialColors.income.copy(alpha = 0.15f)
                        } else {
                            Color(0xFF2A2A2A)
                        }
                    val textColor =
                        if (isSelected) {
                            MaterialTheme.financialColors.income
                        } else {
                            Color(0xFFD1D5DB)
                        }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        border = border,
                        color = bg,
                        modifier = Modifier.clickable { onToggleTag(tag) },
                    ) {
                        Text(
                            text = cleanTag,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inline Add New Tag input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.tag_sheet_new_hint),
                            color = Color(0xFF6B7280),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(100.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.financialColors.income,
                            unfocusedBorderColor = MaterialTheme.financialColors.hairlineDivider,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.financialColors.income,
                        ),
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val trimmed = newTagInput.trim().removePrefix("#")
                        if (trimmed.isNotEmpty()) {
                            onAddNewTag(trimmed)
                            newTagInput = ""
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                            contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                        ),
                    shape = CircleShape,
                    enabled = newTagInput.trim().isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(id = R.string.tag_sheet_add),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
