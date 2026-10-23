package com.kharcha.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen(onContinueClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PrivacyRow(text = stringResource(id = R.string.onboarding_privacy_device))
        Spacer(modifier = Modifier.height(16.dp))
        PrivacyRow(text = stringResource(id = R.string.onboarding_privacy_messages))
        Spacer(modifier = Modifier.height(16.dp))
        PrivacyRow(text = stringResource(id = R.string.onboarding_privacy_delete))
        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = { /* Handle privacy policy link */ }) {
            Text(text = stringResource(id = R.string.onboarding_privacy_link))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.onboarding_continue))
        }
    }
}

@Composable
fun PrivacyRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Placeholder for an icon, e.g., Icon(imageVector = Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
