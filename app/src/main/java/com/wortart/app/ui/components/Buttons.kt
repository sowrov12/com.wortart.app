package com.wortart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.ui.theme.WortArtColors

private val ButtonShape = RoundedCornerShape(14.dp)

/** The main call-to-action: gold gradient with dark text. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fill = if (enabled) {
        Modifier.background(WortArtColors.GoldGradient, ButtonShape)
    } else {
        Modifier.background(WortArtColors.Elevated, ButtonShape)
    }
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .then(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) WortArtColors.OnGold else WortArtColors.TextMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The secondary action: transparent with a gold outline. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val color = if (enabled) WortArtColors.Gold else WortArtColors.TextMuted
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .border(1.5.dp, color.copy(alpha = if (enabled) 0.8f else 0.4f), ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
