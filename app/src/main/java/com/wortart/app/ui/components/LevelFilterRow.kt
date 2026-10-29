package com.wortart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.ui.theme.WortArtColors

/** A rounded pill that can be selected, used for filters. */
@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) WortArtColors.Gold else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) WortArtColors.Gold else WortArtColors.Gold.copy(alpha = 0.5f),
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) WortArtColors.OnGold else WortArtColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** "All, A1, A2, B1, B2" filter chips. [selected] is null for "All". */
@Composable
fun LevelFilterRow(selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillChip(label = "All", selected = selected == null, onClick = { onSelect(null) })
        CefrLevels.ALL.forEach { level ->
            PillChip(label = level, selected = selected == level, onClick = { onSelect(level) })
        }
    }
}
