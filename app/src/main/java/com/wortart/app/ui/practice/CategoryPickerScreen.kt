package com.wortart.app.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.ui.components.WortArtTopBar
import com.wortart.app.ui.explore.topicEmoji
import com.wortart.app.ui.theme.WortArtColors

/** A separate page with one box per category; tapping a box returns to the flashcards. */
@Composable
fun CategoryPickerScreen(
    counts: GroupCounts,
    selected: String?,
    onLoad: () -> Unit,
    onSelect: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { onLoad() }

    Column(modifier = modifier.fillMaxSize()) {
        WortArtTopBar(title = "Category", onBack = onBack)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "all") {
                CategoryBox(
                    name = "All Category",
                    emoji = "🗂",
                    count = counts.total,
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
            }
            items(counts.groups, key = { it.first }) { entry ->
                CategoryBox(
                    name = entry.first,
                    emoji = topicEmoji(entry.first),
                    count = entry.second,
                    selected = selected == entry.first,
                    onClick = { onSelect(entry.first) },
                )
            }
        }
    }
}

@Composable
private fun CategoryBox(
    name: String,
    emoji: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        if (selected) WortArtColors.Gold.copy(alpha = 0.22f) else WortArtColors.Elevated,
                        WortArtColors.Surface,
                    ),
                ),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) WortArtColors.Gold else WortArtColors.Border,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(WortArtColors.Gold.copy(alpha = 0.16f), CircleShape)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = name,
                color = if (selected) WortArtColors.Gold else WortArtColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(text = "$count words", color = WortArtColors.TextMuted, fontSize = 12.sp)
        }
    }
}
