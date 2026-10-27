package com.kharcha.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Pinned transaction form footer with "Save & add another" on left and lime "✓ Save" pill on right.
 */
@Composable
fun FormFooter(
    onSaveAndAddAnother: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    isSaveEnabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onSaveAndAddAnother,
            enabled = isSaveEnabled,
        ) {
            Text(
                text = stringResource(id = R.string.form_footer_save_and_add),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color =
                    if (isSaveEnabled) {
                        MaterialTheme.financialColors.income
                    } else {
                        MaterialTheme.financialColors.income.copy(alpha = 0.4f)
                    },
            )
        }

        Button(
            onClick = onSave,
            enabled = isSaveEnabled,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                    contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                    disabledContainerColor = MaterialTheme.financialColors.paleLimeAddButton.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.financialColors.onPaleLimeAddButton.copy(alpha = 0.3f),
                ),
            shape = CircleShape,
            modifier = Modifier.height(44.dp),
        ) {
            Text(
                text = stringResource(id = R.string.form_footer_save),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }
}
