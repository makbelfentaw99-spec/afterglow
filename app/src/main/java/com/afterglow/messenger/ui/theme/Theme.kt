package com.afterglow.messenger.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AfterglowColorScheme = darkColorScheme(
    primary = EmberPrimary,
    onPrimary = TextOnEmber,
    primaryContainer = EmberPrimaryMuted,
    onPrimaryContainer = EmberPrimary,
    background = InkBackground,
    onBackground = TextPrimary,
    surface = InkSurface,
    onSurface = TextPrimary,
    surfaceVariant = InkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextOnEmber
)

// Dark-mode-only by design: this is a messaging app, most of its usage is
// glancing at a phone at odd hours, and it keeps the "Afterglow" concept
// coherent rather than forking the palette for a light variant too.
@Composable
fun AfterglowTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = AfterglowColorScheme,
        typography = AfterglowTypography,
        content = content
    )
}
