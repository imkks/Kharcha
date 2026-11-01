package com.kharcha.core.common.security

enum class AuthenticatorPromptMode {
    BIOMETRIC_ONLY,
    BIOMETRIC_WITH_DEVICE_CREDENTIAL,
    LEGACY_DEVICE_CREDENTIAL_FALLBACK,
}

fun authenticatorPromptMode(apiLevel: Int, hasDeviceCredential: Boolean): AuthenticatorPromptMode =
    when {
        !hasDeviceCredential -> AuthenticatorPromptMode.BIOMETRIC_ONLY
        apiLevel >= 30 -> AuthenticatorPromptMode.BIOMETRIC_WITH_DEVICE_CREDENTIAL
        else -> AuthenticatorPromptMode.LEGACY_DEVICE_CREDENTIAL_FALLBACK
    }
