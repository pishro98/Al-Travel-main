package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF), // iOS Blue Dark
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0A84FF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF8E8E93), // System Gray
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1C1C1E),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFF9F0A), // System Orange
    background = Color(0xFF000000), // Pure Black for iOS Dark Mode
    surface = Color(0xFF1C1C1E), // Elevated Dark
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E), // Card/Group background
    onSurfaceVariant = Color(0xFFEBEBF5),
    outline = Color(0xFF38383A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF), // iOS Blue Light
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF007AFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF8E8E93), // System Gray
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2F2F7), // Grouped Background
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFFFF9500), // System Orange
    background = Color(0xFFF2F2F7), // Grouped Light App background
    surface = Color(0xFFFFFFFF), // Cards/Cells
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF3C3C43).copy(alpha = 0.6f),
    outline = Color(0xFFC6C6C8) // Borders/Separators
)

val SleekShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp), // iOS standard corners
    large = RoundedCornerShape(16.dp), // For main grouped cards
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled dynamic to enforce sleek theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SleekShapes,
        content = content
    )
}
