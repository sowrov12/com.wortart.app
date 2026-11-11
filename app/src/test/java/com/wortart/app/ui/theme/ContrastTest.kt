package com.wortart.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG contrast of the design tokens: 4.5:1 for normal text, 3:1 for large text and icons. */
class ContrastTest {

    private fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private val surfaces = mapOf(
        "Background" to WortArtColors.Background,
        "SurfaceDark" to WortArtColors.SurfaceDark,
        "Surface" to WortArtColors.Surface,
        "Elevated" to WortArtColors.Elevated,
    )

    private fun assertReadable(name: String, text: Color, minimum: Double = 4.5) {
        surfaces.forEach { (surfaceName, surface) ->
            val ratio = contrast(text, surface)
            assertTrue("$name on $surfaceName is only %.2f:1".format(ratio), ratio >= minimum)
        }
    }

    @Test
    fun bodyTextIsReadableOnEverySurface() {
        assertReadable("TextPrimary", WortArtColors.TextPrimary)
        assertReadable("TextSecondary", WortArtColors.TextSecondary)
        assertReadable("TextMuted", WortArtColors.TextMuted)
    }

    @Test
    fun accentTextIsReadableOnEverySurface() {
        assertReadable("Gold", WortArtColors.Gold)
        assertReadable("GoldLight (Bengali text)", WortArtColors.GoldLight)
        assertReadable("Correct", WortArtColors.Correct)
        assertReadable("Wrong", WortArtColors.Wrong)
    }

    @Test
    fun darkTextOnGoldButtonsIsReadable() {
        assertTrue(contrast(WortArtColors.OnGold, WortArtColors.Gold) >= 4.5)
        assertTrue(contrast(WortArtColors.OnGold, WortArtColors.GoldGradientStart) >= 4.5)
        assertTrue(contrast(WortArtColors.OnGold, WortArtColors.GoldGradientEnd) >= 4.5)
    }

    @Test
    fun levelBadgesAreReadable() {
        listOf("A1", "A2", "B1", "B2").forEach { level ->
            val ratio = contrast(WortArtColors.OnGold, levelColor(level))
            assertTrue("$level badge is only %.2f:1".format(ratio), ratio >= 4.5)
        }
    }

    @Test
    fun selectedOptionsAreEasyToTellFromUnselectedOnes() {
        // The gold border of a selected answer against the card it sits on (graphics need 3:1).
        assertTrue(contrast(WortArtColors.Gold, WortArtColors.Surface) >= 3.0)
        assertTrue(contrast(WortArtColors.Correct, WortArtColors.Surface) >= 3.0)
        assertTrue(contrast(WortArtColors.Wrong, WortArtColors.Surface) >= 3.0)
    }
}
