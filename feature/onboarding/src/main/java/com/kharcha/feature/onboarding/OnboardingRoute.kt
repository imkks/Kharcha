package com.kharcha.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class OnboardingState {
    WELCOME,
    PRIVACY,
    PERMISSION,
    SETUP,
    IMPORT_PROGRESS,
}

@Composable
fun OnboardingRoute(onOnboardingComplete: () -> Unit = {}) {
    var currentState by remember { mutableStateOf(OnboardingState.WELCOME) }
    var isPermissionDenied by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.0f) }
    var transactionCount by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val isGranted = permissions.values.all { it }
            if (isGranted) {
                currentState = OnboardingState.SETUP
            } else {
                isPermissionDenied = true
            }
        }

    when (currentState) {
        OnboardingState.WELCOME -> {
            WelcomeScreen(onGetStartedClick = { currentState = OnboardingState.PRIVACY })
        }
        OnboardingState.PRIVACY -> {
            PrivacyScreen(onContinueClick = { currentState = OnboardingState.PERMISSION })
        }
        OnboardingState.PERMISSION -> {
            PermissionScreen(
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                        ),
                    )
                },
                onUseManualMode = {
                    currentState = OnboardingState.SETUP
                },
                onOpenSettings = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                },
                isDenied = isPermissionDenied,
            )
        }
        OnboardingState.SETUP -> {
            SetupScreen(onStartImport = { window, storeRaw ->
                currentState = OnboardingState.IMPORT_PROGRESS
            })
        }
        OnboardingState.IMPORT_PROGRESS -> {
            ImportProgressScreen(
                progress = progress,
                transactionCount = transactionCount,
                onSkipClick = onOnboardingComplete,
            )
        }
    }
}
