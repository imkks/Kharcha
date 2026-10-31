package com.kharcha.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.MutableState

/**
 * CompositionLocal indicating whether the privacy shutter (balance masking) is enabled.
 * When true, financial components (such as AmountText and SummaryCard) mask amounts
 * with "₹ ••••••" to prevent shoulder surfing and screen snooping.
 * Follows UI Spec Section 5.11 & Research Recommendation #7.
 */
val LocalPrivacyMode = compositionLocalOf { false }
val LocalPrivacyModeState = compositionLocalOf<MutableState<Boolean>?> { null }

/** Whether income values should be masked independently from the general privacy shutter. */
val LocalIncomeHidden = compositionLocalOf<MutableState<Boolean>?> { null }
val LocalIncomeRevealed = compositionLocalOf<MutableState<Boolean>?> { null }
val LocalIncomeReveal = compositionLocalOf<(() -> Unit)?> { null }
