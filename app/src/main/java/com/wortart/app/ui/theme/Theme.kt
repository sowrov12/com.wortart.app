package com.wortart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WortArtDarkColors = darkColorScheme(
    primary = WortArtColors.Gold,
    onPrimary = WortArtColors.OnGold,
    secondary = WortArtColors.GoldLight,
    background = WortArtColors.Background,
    onBackground = WortArtColors.TextPrimary,
    surface = WortArtColors.Surface,
    onSurface = WortArtColors.TextPrimary,
)

/** App-wide theme. The app is dark-only. */
@Composable
fun WortArtTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WortArtDarkColors, content = content)
}
