package com.example.demonstator2_databases.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrangeSecondary,
    secondary = BeerAmber,
    tertiary = OrangeTertiary,
    background = Color(0xFF2C1810),
    surface = Color(0xFF3D2818),
    surfaceVariant = Color(0xFF4A3320)
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = BeerAmber,
    tertiary = BeerCream,
    background = BeerBackground,
    // Keep things beer-like but readable: slightly lighter surfaces with strong text contrast
    surface = BeerCream,
    surfaceVariant = BeerCard,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = BeerText,
    onSurface = BeerText,
    onSurfaceVariant = BeerTextMuted
)

@Composable
fun Demonstator2databasesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}