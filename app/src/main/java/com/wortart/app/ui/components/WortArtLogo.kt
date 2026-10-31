package com.wortart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wortart.app.ui.theme.WortArtColors

/** The WortArt logo: an open book in gold, drawn on a Canvas. */
@Composable
fun WortArtLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Left page; the right page is its mirror image.
        val left = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            cubicTo(w * 0.36f, h * 0.16f, w * 0.16f, h * 0.16f, w * 0.04f, h * 0.24f)
            lineTo(w * 0.04f, h * 0.80f)
            cubicTo(w * 0.16f, h * 0.72f, w * 0.36f, h * 0.72f, w * 0.50f, h * 0.84f)
            close()
        }
        val right = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            cubicTo(w * 0.64f, h * 0.16f, w * 0.84f, h * 0.16f, w * 0.96f, h * 0.24f)
            lineTo(w * 0.96f, h * 0.80f)
            cubicTo(w * 0.84f, h * 0.72f, w * 0.64f, h * 0.72f, w * 0.50f, h * 0.84f)
            close()
        }
        drawPath(path = left, brush = WortArtColors.GoldGradient)
        drawPath(path = right, brush = WortArtColors.GoldGradient)

        // Page lines and the spine.
        val line = w * 0.025f
        for (i in 0..2) {
            val y = h * (0.40f + i * 0.11f)
            drawLine(WortArtColors.Background, Offset(w * 0.12f, y), Offset(w * 0.42f, y + h * 0.02f), strokeWidth = line, cap = StrokeCap.Round)
            drawLine(WortArtColors.Background, Offset(w * 0.58f, y + h * 0.02f), Offset(w * 0.88f, y), strokeWidth = line, cap = StrokeCap.Round)
        }
        drawLine(WortArtColors.Background, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.50f, h * 0.84f), strokeWidth = line * 1.4f)
    }
}
