package com.kharcha.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = axio_pale_lime,
        onPrimaryContainer = axio_background_dark,
        background = axio_background_dark,
        onBackground = axio_text_primary,
        surface = axio_surface_dark,
        onSurface = axio_text_primary,
        surfaceVariant = axio_surface_highest,
        onSurfaceVariant = axio_text_secondary,
        surfaceContainer = axio_surface_dark,
        surfaceContainerHigh = axio_surface_high,
        surfaceContainerHighest = axio_surface_highest,
        outline = axio_hairline_divider,
        outlineVariant = axio_hairline_divider,
    )

private val DarkColors =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = axio_background_dark,
        primaryContainer = axio_pale_lime,
        onPrimaryContainer = axio_background_dark,
        background = axio_background_dark,
        onBackground = axio_text_primary,
        surface = axio_surface_dark,
        onSurface = axio_text_primary,
        surfaceVariant = axio_surface_highest,
        onSurfaceVariant = axio_text_secondary,
        surfaceContainer = axio_surface_dark,
        surfaceContainerHigh = axio_surface_high,
        surfaceContainerHighest = axio_surface_highest,
        outline = axio_hairline_divider,
        outlineVariant = axio_hairline_divider,
    )

@Composable
fun KharchaTheme(
    useDarkTheme: Boolean = true,
    content:
        @Composable()
        () -> Unit,
) {
    val colors =
        if (!useDarkTheme) {
            LightColors
        } else {
            DarkColors
        }

    val financialColors =
        if (!useDarkTheme) {
            LightFinancialColors
        } else {
            DarkFinancialColors
        }

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = KharchaTypography,
            content = content,
        )
    }
}
