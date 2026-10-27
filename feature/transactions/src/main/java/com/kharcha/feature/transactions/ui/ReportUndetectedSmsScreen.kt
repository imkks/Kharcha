package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.feature.transactions.R

/**
 * Report Undetected SMS Screen (UI Spec Section 7.6).
 * Form allowing users to report missed bank SMS to improve detection.
 * Strictly adheres to privacy: zero internet, sensitive details masked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUndetectedSmsScreen(
    onSubmitReport: (sender: String, rawSms: String, maskDetails: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rawSmsInput by remember { mutableStateOf("") }
    var senderInput by remember { mutableStateOf("") }
    var maskDetails by remember { mutableStateOf(true) }

    val isSubmitEnabled = rawSmsInput.isNotBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.report_sms_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.financialColors.textPrimary,
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
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Button(
                        onClick = {
                            if (isSubmitEnabled) {
                                onSubmitReport(senderInput.trim(), rawSmsInput.trim(), maskDetails)
                                onDismiss()
                            }
                        },
                        enabled = isSubmitEnabled,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.limeAccent,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                                disabledContainerColor = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.financialColors.onPaleLimeAddButton.copy(alpha = 0.3f),
                            ),
                        shape = RoundedCornerShape(100.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.report_sms_submit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
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
            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(
                text = stringResource(id = R.string.report_sms_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.financialColors.textSecondary,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Large Multi-line Text Area for SMS
            OutlinedTextField(
                value = rawSmsInput,
                onValueChange = { rawSmsInput = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.report_sms_body_hint),
                        color = MaterialTheme.financialColors.textSecondary.copy(alpha = 0.5f),
                    )
                },
                minLines = 6,
                maxLines = 10,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.financialColors.limeAccent,
                        unfocusedBorderColor = MaterialTheme.financialColors.hairlineDivider,
                        focusedTextColor = MaterialTheme.financialColors.textPrimary,
                        unfocusedTextColor = MaterialTheme.financialColors.textPrimary,
                    ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Optional Sender Header Input
            Text(
                text = stringResource(id = R.string.report_sms_sender_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.financialColors.textSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = senderInput,
                onValueChange = { senderInput = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.report_sms_sender_hint),
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = maskDetails,
                    onCheckedChange = { maskDetails = it },
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.financialColors.limeAccent,
                            checkmarkColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            uncheckedColor = MaterialTheme.financialColors.textSecondary,
                        ),
                )
                Text(
                    text = stringResource(id = R.string.report_sms_mask_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.financialColors.textSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
