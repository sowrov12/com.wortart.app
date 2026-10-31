package com.wortart.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Design tokens of the dark-and-gold WortArt style (see PLAN: "Shared Design Tokens"). */
object WortArtColors {
    // Backgrounds, from darkest to lightest
    val Background = Color(0xFF080D13)   // primary background
    val SurfaceDark = Color(0xFF121820)  // secondary background: insets inside cards, bars
    val Surface = Color(0xFF151C25)      // card background
    val Elevated = Color(0xFF202833)     // elevated surface: chips, unselected options
    val Border = Color(0xFF34404D)

    // Gold
    val Gold = Color(0xFFFFB52E)         // primary accent
    val GoldSecondary = Color(0xFFE5A93C)
    val GoldLight = Color(0xFFF2D08A)    // soft gold, used for Bengali text
    val GoldGradientStart = Color(0xFFFFD15C)
    val GoldGradientEnd = Color(0xFFF5A623)
    val OnGold = Color(0xFF080D13)

    // Text
    val TextPrimary = Color(0xFFF7F5F0)
    val TextSecondary = Color(0xFFA8B0BC)
    val TextMuted = Color(0xFF7C8796)

    // Feedback
    val Correct = Color(0xFF20C997)
    val Wrong = Color(0xFFFF5C64)

    /** Gold gradient for primary buttons and progress bars. */
    val GoldGradient: Brush
        get() = Brush.horizontalGradient(listOf(GoldGradientStart, GoldGradientEnd))
}

/** The badge color of a CEFR level. */
fun levelColor(level: String): Color = when (level) {
    "A1" -> Color(0xFF20C997)
    "A2" -> Color(0xFFFFB52E)
    "B1" -> Color(0xFFFF8A3D)
    "B2" -> Color(0xFFFF6B9A)
    else -> Color(0xFFA8B0BC)
}
