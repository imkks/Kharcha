package com.kharcha.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

enum class OnboardingState {
    WELCOME,
    PRIVACY,
    PERMISSION,
    SETUP,
    IMPORT_PROGRESS,
}

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val hasSms =
                permissions[Manifest.permission.READ_SMS] == true ||
                    permissions[Manifest.permission.RECEIVE_SMS] == true
            viewModel.onPermissionResult(hasSms)
        }

    when (uiState.currentState) {
        OnboardingState.WELCOME -> {
            WelcomeScreen(onGetStartedClick = { viewModel.moveToState(OnboardingState.PRIVACY) })
        }
        OnboardingState.PRIVACY -> {
            PrivacyScreen(onContinueClick = { viewModel.moveToState(OnboardingState.PERMISSION) })
        }
        OnboardingState.PERMISSION -> {
            PermissionScreen(
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                onUseManualMode = {
                    viewModel.moveToState(OnboardingState.SETUP)
                },
                onOpenSettings = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                },
                isDenied = uiState.isPermissionDenied,
            )
        }
        OnboardingState.SETUP -> {
            SetupScreen(onStartImport = { window, storeRaw ->
                viewModel.startImport(window, storeRaw)
            })
        }
        OnboardingState.IMPORT_PROGRESS -> {
            ImportProgressScreen(
                progress = uiState.progress,
                transactionCount = uiState.transactionCount,
                isComplete = uiState.isImportComplete || uiState.progress >= 1.0f,
                onSkipClick = onOnboardingComplete,
            )
        }
    }
}
