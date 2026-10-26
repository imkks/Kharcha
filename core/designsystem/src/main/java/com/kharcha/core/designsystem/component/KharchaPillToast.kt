package com.kharcha.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Axio-style pill-shaped dark feedback toast (UI Specification Section 1 & Feedback).
 * Renders a compact dark pill container with a small lime green icon and message.
 */
@Composable
fun KharchaPillToast(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CheckCircle,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.financialColors.cardBackground,
        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.financialColors.limeAccent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = MaterialTheme.financialColors.textPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = actionLabel,
                    color = MaterialTheme.financialColors.limeAccent,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.clickable { onActionClick() },
                )
            }
        }
    }
}

/**
 * Adapter to render [SnackbarData] as a [KharchaPillToast] inside standard [androidx.compose.material3.SnackbarHost].
 */
@Composable
fun KharchaPillSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CheckCircle,
) {
    KharchaPillToast(
        message = snackbarData.visuals.message,
        modifier = modifier,
        icon = icon,
        actionLabel = snackbarData.visuals.actionLabel,
        onActionClick = { snackbarData.performAction() },
    )
}
