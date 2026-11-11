package com.wortart.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** The one way every screen shows "loading": a gold spinner with an optional line of text. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = WortArtColors.Gold)
        if (message != null) {
            Text(text = message, color = WortArtColors.TextSecondary, fontSize = 15.sp)
        }
    }
}

/**
 * The one way every screen shows "nothing here" or "something went wrong": an optional emoji,
 * a title, an optional explanation and an optional action button.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 36.sp)
        }
        Text(
            text = title,
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            OutlineButton(text = actionLabel, onClick = onAction)
        }
    }
}
