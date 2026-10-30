package com.kharcha.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.theme.KharchaTheme
import com.kharcha.core.designsystem.theme.financialColors

@Composable
fun ProfileScreen(onContinue: (name: String, phone: String) -> Unit) {
    val colors = MaterialTheme.financialColors
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val phoneDigitCount = phone.count(Char::isDigit)
    val canContinue = name.isNotBlank() && phoneDigitCount in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_profile_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_profile_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.onboarding_profile_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.onboarding_profile_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = { onContinue(name.trim(), phone.trim()) },
            enabled = canContinue,
            shape = CircleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.limeAccent,
                    contentColor = colors.onLimeAccent,
                ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(R.string.onboarding_profile_continue))
        }
    }
}

private const val MIN_PHONE_DIGITS = 7
private const val MAX_PHONE_DIGITS = 15

@Preview(name = "Profile light")
@Composable
private fun ProfileScreenLightPreview() {
    KharchaTheme(useDarkTheme = false) {
        ProfileScreen(onContinue = { _, _ -> })
    }
}

@Preview(name = "Profile dark")
@Composable
private fun ProfileScreenDarkPreview() {
    KharchaTheme(useDarkTheme = true) {
        ProfileScreen(onContinue = { _, _ -> })
    }
}

@Preview(name = "Profile large text", fontScale = 2f)
@Composable
private fun ProfileScreenLargeTextPreview() {
    KharchaTheme(useDarkTheme = false) {
        ProfileScreen(onContinue = { _, _ -> })
    }
}
