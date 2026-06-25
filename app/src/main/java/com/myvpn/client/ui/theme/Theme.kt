package com.myvpn.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

val VpnGreen = Color(0xFF00E676)
val VpnOrange = Color(0xFFFF9100)
val VpnRed = Color(0xFFFF1744)

// Fallback dark (no Material You)
private val FallbackDarkScheme = darkColorScheme(
    primary = Color(0xFF6C9FFF),
    secondary = Color(0xFF82B1FF),
    tertiary = Color(0xFFB388FF),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF1C2333),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E)
)

// Fallback light
private val FallbackLightScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    secondary = Color(0xFF4285F4),
    tertiary = Color(0xFF7C4DFF),
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
    onSurfaceVariant = Color(0xFF656D76)
)

enum class ThemeMode {
    LIGHT, DARK, AMOLED
}

@Composable
fun MyVPNClientTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val hasDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> {
            if (hasDynamic) dynamicLightColorScheme(context)
            else FallbackLightScheme
        }
        ThemeMode.DARK -> {
            if (hasDynamic) dynamicDarkColorScheme(context)
            else FallbackDarkScheme
        }
        ThemeMode.AMOLED -> {
            val base = if (hasDynamic) dynamicDarkColorScheme(context) else FallbackDarkScheme
            // Override backgrounds to pure black for AMOLED
            base.copy(
                background = Color.Black,
                surface = Color(0xFF050505),
                surfaceVariant = Color(0xFF111111)
            )
        }
    }

    val animatedColorScheme = colorScheme.copy(
        primary = animateColorAsState(colorScheme.primary, tween(500), label = "primary").value,
        secondary = animateColorAsState(colorScheme.secondary, tween(500), label = "secondary").value,
        tertiary = animateColorAsState(colorScheme.tertiary, tween(500), label = "tertiary").value,
        background = animateColorAsState(colorScheme.background, tween(500), label = "background").value,
        surface = animateColorAsState(colorScheme.surface, tween(500), label = "surface").value,
        surfaceVariant = animateColorAsState(colorScheme.surfaceVariant, tween(500), label = "surfaceVariant").value,
        onBackground = animateColorAsState(colorScheme.onBackground, tween(500), label = "onBg").value,
        onSurface = animateColorAsState(colorScheme.onSurface, tween(500), label = "onSurface").value,
        onSurfaceVariant = animateColorAsState(colorScheme.onSurfaceVariant, tween(500), label = "onSurfaceVar").value,
        primaryContainer = animateColorAsState(colorScheme.primaryContainer, tween(500), label = "primaryCont").value,
        onPrimaryContainer = animateColorAsState(colorScheme.onPrimaryContainer, tween(500), label = "onPrimaryCont").value,
        errorContainer = animateColorAsState(colorScheme.errorContainer, tween(500), label = "errorCont").value,
        outline = animateColorAsState(colorScheme.outline, tween(500), label = "outline").value,
        outlineVariant = animateColorAsState(colorScheme.outlineVariant, tween(500), label = "outlineVar").value
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        content = content
    )
}
