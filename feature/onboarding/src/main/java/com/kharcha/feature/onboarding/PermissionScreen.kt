package com.kharcha.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors

@Composable
fun PermissionScreen(
    onRequestPermission: (selectedWindow: String) -> Unit,
    onUseManualMode: () -> Unit,
    onOpenSettings: () -> Unit,
    isDenied: Boolean,
) {
    val colors = MaterialTheme.financialColors
    val scrollState = rememberScrollState()
    var selectedWindow by remember { mutableStateOf("90 days") }

    val windowOptions =
        listOf(
            "30 days" to stringResource(id = R.string.onboarding_setup_30_days),
            "90 days" to stringResource(id = R.string.onboarding_setup_90_days),
            "6 months" to stringResource(id = R.string.onboarding_setup_6_months),
            "All" to stringResource(id = R.string.onboarding_setup_all),
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // SMS Icon Badge
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.limeAccent.copy(alpha = 0.12f))
                        .border(1.dp, colors.limeAccent.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = DesignR.drawable.ic_message_sms),
                    contentDescription = null,
                    tint = colors.limeAccent,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.onboarding_permission_title),
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.onboarding_permission_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permission Points Card
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.hairlineDivider, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PermissionBulletRow(
                    text = stringResource(id = R.string.onboarding_permission_point_automated),
                    colors = colors,
                )

                HorizontalDivider(color = colors.hairlineDivider, thickness = 1.dp)

                PermissionBulletRow(
                    text = stringResource(id = R.string.onboarding_permission_point_accounts),
                    colors = colors,
                )

                HorizontalDivider(color = colors.hairlineDivider, thickness = 1.dp)

                PermissionBulletRow(
                    text = stringResource(id = R.string.onboarding_permission_point_private),
                    colors = colors,
                )

                HorizontalDivider(color = colors.hairlineDivider, thickness = 1.dp)

                PermissionBulletRow(
                    text = stringResource(id = R.string.onboarding_permission_point_location),
                    colors = colors,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Import Window Selector Card
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.hairlineDivider, RoundedCornerShape(18.dp))
                        .padding(16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_import_window_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    windowOptions.forEach { (key, label) ->
                        val isSelected = selectedWindow == key
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) colors.limeAccent else colors.raisedRowBackground,
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.dp, colors.limeAccent, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, colors.hairlineDivider, CircleShape)
                                        },
                                    )
                                    .clickable { selectedWindow = key }
                                    .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) colors.onLimeAccent else colors.textSecondary,
                            )
                        }
                    }
                }
            }

            if (isDenied) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.warningContainer.copy(alpha = 0.15f))
                            .border(1.dp, colors.warning.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_permission_denied_help),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = colors.warning,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { onRequestPermission(selectedWindow) },
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.limeAccent,
                        contentColor = colors.onLimeAccent,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_permission_cta),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            if (isDenied) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_open_settings),
                        color = colors.textPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onUseManualMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_use_manual),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PermissionBulletRow(
    text: String,
    colors: com.kharcha.core.designsystem.theme.FinancialColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.limeAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = DesignR.drawable.ic_shield_check),
                contentDescription = null,
                tint = colors.limeAccent,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
