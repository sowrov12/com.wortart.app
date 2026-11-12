package com.wortart.app.ui.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.R
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.components.OutlineButton
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** The first screen after installing: Brandenburg Gate at night, logo, tagline, "Get Started" and "Login". */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(WortArtColors.Background)) {
        Image(
            painter = painterResource(R.drawable.welcome_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Darkens the sky a little and the ground a lot, so the text and buttons stay readable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x55000000),
                        0.35f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to Color(0xF2000000),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.6f))

            Image(
                painter = painterResource(R.drawable.welcome_logo),
                contentDescription = "WortArt",
                modifier = Modifier.size(132.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "WortArt",
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFF5A623))),
                    fontSize = 54.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "German Grammar & Vocab Master",
                color = WortArtColors.TextPrimary,
                fontSize = 17.sp,
                fontFamily = WortArtFonts.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))
            FlagDivider()
            Spacer(Modifier.height(18.dp))

            Text(
                text = "Mehr lernen. Mehr können.",
                color = WortArtColors.Gold,
                fontSize = 20.sp,
                fontFamily = WortArtFonts.Serif,
                fontStyle = FontStyle.Italic,
            )

            Spacer(Modifier.weight(1.4f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GoldButton(text = "Get Started", onClick = onGetStarted, modifier = Modifier.fillMaxWidth())
                OutlineButton(text = "Login", onClick = onLogin, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** A thin gold line with the three colors of the German flag in the middle. */
@Composable
private fun FlagDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, WortArtColors.Gold))),
        )
        Canvas(modifier = Modifier.size(width = 46.dp, height = 10.dp)) {
            val barWidth = size.width / 3f
            val skew = size.height * 0.5f
            listOf(Color(0xFF111111), Color(0xFFDD0000), Color(0xFFFFCE00)).forEachIndexed { index, color ->
                val x = index * barWidth
                val path = Path().apply {
                    moveTo(x + skew, 0f)
                    lineTo(x + barWidth - 1f + skew, 0f)
                    lineTo(x + barWidth - 1f, size.height)
                    lineTo(x, size.height)
                    close()
                }
                drawPath(path, color)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(WortArtColors.Gold, Color.Transparent))),
        )
    }
}
