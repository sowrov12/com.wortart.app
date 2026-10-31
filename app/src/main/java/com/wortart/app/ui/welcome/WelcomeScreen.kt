package com.wortart.app.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.components.OutlineButton
import com.wortart.app.ui.components.WortArtLogo
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** The first screen after installing: logo, tagline, "Get Started" and "Login". */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        WortArtColors.Background,
                        Color(0xFF0D141C),
                        Color(0xFF2B1F0D),
                    ),
                ),
            )
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        WortArtLogo()
        Spacer(Modifier.height(12.dp))
        Text(
            text = "WortArt",
            color = WortArtColors.GoldLight,
            fontSize = 48.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "German Grammar & Vocab Master",
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
            fontFamily = WortArtFonts.Serif,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))
        Text(
            text = "Mehr lernen. Mehr können.",
            color = WortArtColors.Gold,
            fontSize = 18.sp,
            fontFamily = WortArtFonts.Serif,
            fontStyle = FontStyle.Italic,
        )

        Spacer(Modifier.weight(1.2f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoldButton(text = "Get Started", onClick = onGetStarted, modifier = Modifier.fillMaxWidth())
            OutlineButton(text = "Login", onClick = onLogin, modifier = Modifier.fillMaxWidth())
        }
    }
}
