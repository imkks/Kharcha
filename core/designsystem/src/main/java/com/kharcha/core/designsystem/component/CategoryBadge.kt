package com.kharcha.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CategoryBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val initial = categoryName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "•"
    val colorIndex = (initial.firstOrNull()?.code ?: 0).rem(3)
    val containerColor =
        when (colorIndex) {
            1 -> MaterialTheme.colorScheme.secondaryContainer
            2 -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        }
    val contentColor =
        when (colorIndex) {
            1 -> MaterialTheme.colorScheme.onSecondaryContainer
            2 -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }

    val iconRes = CategoryIconHelper.getCategoryIcon(categoryName)

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = categoryName,
            tint = contentColor,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}
