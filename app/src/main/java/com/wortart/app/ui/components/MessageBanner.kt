package com.wortart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.ui.theme.WortArtColors

/** A short notice or error with an OK button. */
@Composable
fun MessageBanner(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.SurfaceDark)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.4f), shape)
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = WortArtColors.TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text(text = "OK", color = WortArtColors.Gold)
        }
    }
}
