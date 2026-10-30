package com.kharcha.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

enum class OnboardingState {
    WELCOME,
    PERMISSION,
    IMPORT_PROGRESS,
}

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingImportWindow by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("90 days") }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val hasSms =
                permissions[Manifest.permission.READ_SMS] == true ||
                    permissions[Manifest.permission.RECEIVE_SMS] == true
            viewModel.onPermissionResult(hasSms, pendingImportWindow)
        }

    when (uiState.currentState) {
        OnboardingState.WELCOME -> {
            WelcomeScreen(
                onGetStartedClick = { viewModel.moveToState(OnboardingState.PERMISSION) },
            )
        }
        OnboardingState.PERMISSION -> {
            PermissionScreen(
                onRequestPermission = { chosenWindow ->
                    pendingImportWindow = chosenWindow
                    val permissions =
                        mutableListOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                },
                onUseManualMode = onOnboardingComplete,
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
