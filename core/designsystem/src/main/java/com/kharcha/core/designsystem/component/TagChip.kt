package com.kharcha.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Light-blue pill chip with an embedded count badge for tag clouds (e.g. #Online 22, #Rent 1).
 */
@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val bg =
        if (isSelected) {
            MaterialTheme.financialColors.onlineTagText.copy(alpha = 0.25f)
        } else {
            MaterialTheme.financialColors.onlineTagBackground
        }
    val textColor = MaterialTheme.financialColors.onlineTagText

    val cleanTag = if (tag.startsWith("#")) tag else "#$tag"

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(100.dp))
                .background(bg)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = cleanTag,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )

        if (count != null && count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }
    }
}
