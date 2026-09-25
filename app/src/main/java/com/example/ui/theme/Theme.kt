package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Cyan200,
    secondary = Emerald400,
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    onSecondaryContainer = Color(0xFF6FF7B4),
    tertiary = Amber400,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    error = Red500
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00687A),
    onPrimary = Color.White,
    primaryContainer = Cyan200,
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF006C47),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF8CF5BF),
    onSecondaryContainer = Color(0xFF002113),
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Slate700,
    error = Red600
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to night-mode for surveillance app
    dynamicColor: Boolean = false, // Keep consistent high-contrast surveillance palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
