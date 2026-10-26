package com.kharcha.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Data representation for an item in the [AxioBottomNavBar].
 */
data class AxioNavItem(
    val icon: Painter,
    val contentDescription: String,
) {
    constructor(
        vector: ImageVector,
        contentDescription: String,
    ) : this(
        icon = null as Painter? ?: error("Unused"), // delegated below
        contentDescription = contentDescription,
    )
}

/**
 * Convenience helper to create [AxioNavItem] with [ImageVector].
 */
@Composable
fun rememberAxioVectorNavItem(
    imageVector: ImageVector,
    contentDescription: String,
): AxioNavItem {
    val painter = rememberVectorPainter(imageVector)
    return remember(imageVector, contentDescription) {
        AxioNavItem(
            icon = painter,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Axio-style dark 4-destination bottom navigation bar (UI Specification Section 2).
 * Features:
 * - 4 icon-only destinations (no labels).
 * - Thin lime glow indicator line above the selected item.
 * - Hairline top divider on dark surface.
 */
@Composable
fun AxioBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    items: List<AxioNavItem>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.financialColors.cardBackground,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.financialColors.hairlineDivider,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val iconColor by animateColorAsState(
                        targetValue =
                            if (isSelected) {
                                MaterialTheme.financialColors.limeAccent
                            } else {
                                MaterialTheme.financialColors.textSecondary
                            },
                        label = "BottomNavIconColor",
                    )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onItemSelected(index) },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (isSelected) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier =
                                        Modifier
                                            .width(36.dp)
                                            .height(3.dp)
                                            .background(
                                                color = MaterialTheme.financialColors.limeAccent,
                                                shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp),
                                            ),
                                )
                                Box(
                                    modifier =
                                        Modifier
                                            .width(44.dp)
                                            .height(4.dp)
                                            .background(
                                                brush =
                                                    Brush.verticalGradient(
                                                        colors =
                                                            listOf(
                                                                MaterialTheme.financialColors.limeAccent.copy(alpha = 0.35f),
                                                                Color.Transparent,
                                                            ),
                                                    ),
                                            ),
                                )
                            }
                        }

                        Icon(
                            painter = item.icon,
                            contentDescription = item.contentDescription,
                            tint = iconColor,
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
