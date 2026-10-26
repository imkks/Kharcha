package com.kharcha.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Deprecated("Removed from onboarding flow.")
@Composable
fun SetupScreen(onStartImport: (window: String, storeRaw: Boolean) -> Unit) {
    var storeRawSms by remember { mutableStateOf(false) }
    var selectedWindow by remember { mutableStateOf("30 days") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_setup_currency),
            style = MaterialTheme.typography.titleMedium,
        )
        // Currency Dropdown placeholder
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.onboarding_setup_import_window),
            style = MaterialTheme.typography.titleMedium,
        )
        // Segmented buttons placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { selectedWindow = "30 days" }) {
                Text(stringResource(id = R.string.onboarding_setup_30_days))
            }
            Button(onClick = { selectedWindow = "90 days" }) {
                Text(stringResource(id = R.string.onboarding_setup_90_days))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { selectedWindow = "6 months" }) {
                Text(stringResource(id = R.string.onboarding_setup_6_months))
            }
            Button(onClick = { selectedWindow = "All" }) {
                Text(stringResource(id = R.string.onboarding_setup_all))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(id = R.string.onboarding_setup_store_original))
            Switch(
                checked = storeRawSms,
                onCheckedChange = { storeRawSms = it },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onStartImport(selectedWindow, storeRawSms) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.onboarding_setup_start_import))
        }
    }
}
