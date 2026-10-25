package com.kharcha.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.theme.CategoryColors

@Composable
fun CategoryBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val colorToken = CategoryColors.forCategory(categoryName)
    val iconRes = CategoryIconHelper.getCategoryIcon(categoryName)

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(colorToken.containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = categoryName,
            tint = colorToken.onContainerColor,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}
