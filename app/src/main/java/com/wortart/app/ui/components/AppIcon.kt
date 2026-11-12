package com.wortart.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.icons.iconKey

/** The icons that were loaded from the icon sheet, by icon key. */
val LocalAppIcons = compositionLocalOf<Map<String, ImageBitmap>> { emptyMap() }

/** The icon [name] from the sheet; [fallback] is shown while it is not on the phone yet. */
@Composable
fun AppIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp,
    fallback: @Composable () -> Unit,
) {
    val bitmap = LocalAppIcons.current[iconKey(name)]
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        fallback()
    }
}

/** Same, with an emoji as the fallback. */
@Composable
fun AppIcon(
    name: String,
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    AppIcon(name = name, modifier = modifier, size = size) {
        Text(text = emoji, fontSize = (size.value * 0.85f).sp)
    }
}
