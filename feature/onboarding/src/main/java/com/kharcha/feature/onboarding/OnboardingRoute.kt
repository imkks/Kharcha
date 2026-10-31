package com.kharcha.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel

enum class OnboardingState {
    PROFILE,
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
    var showBackgroundLocationPrompt by remember { mutableStateOf(false) }
    var pendingSmsPermissionGranted by remember { mutableStateOf(false) }

    val backgroundLocationSettingsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            viewModel.onPermissionResult(pendingSmsPermissionGranted, pendingImportWindow)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val hasSms =
                permissions[Manifest.permission.READ_SMS] == true ||
                    permissions[Manifest.permission.RECEIVE_SMS] == true
            val hasForegroundLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val needsBackgroundLocationSetup =
                hasSms &&
                    hasForegroundLocation &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (needsBackgroundLocationSetup) {
                pendingSmsPermissionGranted = hasSms
                showBackgroundLocationPrompt = true
            } else {
                viewModel.onPermissionResult(hasSms, pendingImportWindow)
            }
        }

    when (uiState.currentState) {
        OnboardingState.PROFILE -> {
            ProfileScreen(onContinue = viewModel::saveProfile)
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

    if (showBackgroundLocationPrompt) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundLocationPrompt = false
                viewModel.onPermissionResult(pendingSmsPermissionGranted, pendingImportWindow)
            },
            title = { Text(text = stringResource(R.string.onboarding_background_location_title)) },
            text = { Text(text = stringResource(R.string.onboarding_background_location_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundLocationPrompt = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        backgroundLocationSettingsLauncher.launch(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.onboarding_background_location_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackgroundLocationPrompt = false
                        viewModel.onPermissionResult(pendingSmsPermissionGranted, pendingImportWindow)
                    },
                ) {
                    Text(text = stringResource(R.string.onboarding_background_location_skip))
                }
            },
        )
    }
}
