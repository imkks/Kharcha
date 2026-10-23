package com.kharcha.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ImportProgressScreen(
    progress: Float,
    transactionCount: Int,
    onSkipClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.onboarding_import_progress_counter, transactionCount),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Animated list placeholder
        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onSkipClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.onboarding_skip_to_app))
        }
    }
}
