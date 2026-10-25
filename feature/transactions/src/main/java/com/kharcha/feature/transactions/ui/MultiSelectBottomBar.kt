package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.feature.transactions.R

@Composable
fun MultiSelectBottomBar(
    @Suppress("UNUSED_PARAMETER") selectedCount: Int,
    onCategorizeClick: () -> Unit,
    onChangeTypeClick: () -> Unit,
    onMarkReviewedClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionBarItem(
                painter = painterResource(id = DesignR.drawable.ic_label),
                label = stringResource(id = R.string.multiselect_categorize),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onCategorizeClick,
            )

            ActionBarItem(
                painter = painterResource(id = DesignR.drawable.ic_category_transfer),
                label = stringResource(id = R.string.multiselect_change_type),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onChangeTypeClick,
            )

            ActionBarItem(
                painter = rememberVectorPainter(Icons.Default.Check),
                label = stringResource(id = R.string.multiselect_mark_reviewed),
                tint = MaterialTheme.financialColors.income,
                onClick = onMarkReviewedClick,
            )

            ActionBarItem(
                painter = rememberVectorPainter(Icons.Default.Delete),
                label = stringResource(id = R.string.multiselect_delete),
                tint = MaterialTheme.financialColors.expense,
                onClick = onDeleteClick,
            )
        }
    }
}

@Composable
private fun ActionBarItem(
    painter: Painter,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tint,
        )
    }
}
