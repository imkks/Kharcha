package com.kharcha.core.common.security

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticatorPromptModeTest {
    @Test
    fun usesCombinedAuthenticatorsOnAndroid11AndNewerWhenDeviceCredentialIsSet() {
        assertEquals(
            AuthenticatorPromptMode.BIOMETRIC_WITH_DEVICE_CREDENTIAL,
            authenticatorPromptMode(apiLevel = 30, hasDeviceCredential = true),
        )
        assertEquals(
            AuthenticatorPromptMode.BIOMETRIC_WITH_DEVICE_CREDENTIAL,
            authenticatorPromptMode(apiLevel = 35, hasDeviceCredential = true),
        )
    }

    @Test
    fun usesLegacyCredentialFallbackBeforeAndroid11() {
        assertEquals(
            AuthenticatorPromptMode.LEGACY_DEVICE_CREDENTIAL_FALLBACK,
            authenticatorPromptMode(apiLevel = 26, hasDeviceCredential = true),
        )
        assertEquals(
            AuthenticatorPromptMode.LEGACY_DEVICE_CREDENTIAL_FALLBACK,
            authenticatorPromptMode(apiLevel = 29, hasDeviceCredential = true),
        )
    }

    @Test
    fun usesBiometricOnlyWhenDeviceHasNoCredential() {
        assertEquals(
            AuthenticatorPromptMode.BIOMETRIC_ONLY,
            authenticatorPromptMode(apiLevel = 35, hasDeviceCredential = false),
        )
    }
}
