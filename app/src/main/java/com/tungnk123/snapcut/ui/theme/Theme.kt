package com.tungnk123.snapcut.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SnapCutOrange80,
    secondary = SnapCutOrangeGrey80,
    tertiary = SnapCutAmber80
)

private val LightColorScheme = lightColorScheme(
    primary = SnapCutOrange40,
    secondary = SnapCutOrangeGrey40,
    tertiary = SnapCutAmber40
)

@Composable
fun SnapCutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color uses device wallpaper on Android 12+ — falls back to custom scheme
    dynamicColor: Boolean = true,
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
