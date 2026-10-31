package com.wortart.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** First word of a display name, or null when there is no usable name. */
fun firstNameOf(displayName: String?): String? =
    displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }

/**
 * The Home tab: greeting, progress at a glance and quick access to the main areas.
 * Stateless; every tile is a callback.
 */
@Composable
fun HomeScreen(
    userName: String?,
    isMember: Boolean,
    summary: StatsSummary,
    onOpenFlashcards: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "WortArt",
                color = WortArtColors.Gold,
                fontSize = 26.sp,
                fontFamily = WortArtFonts.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (isMember) {
                Text(text = "👑", fontSize = 20.sp)
            }
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Open profile",
                    tint = WortArtColors.TextPrimary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Hallo, ${userName ?: "Learner"}!",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontFamily = WortArtFonts.Serif,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Keep going! Your German journey is getting stronger.",
                color = WortArtColors.TextSecondary,
                fontSize = 15.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "German Score",
                value = "${summary.germanScore}",
                modifier = Modifier.weight(1f),
                leading = {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = WortArtColors.Gold, modifier = Modifier.size(30.dp))
                },
            )
            StatTile(
                label = "Daily Streak",
                value = streakLabel(summary.streakDays),
                modifier = Modifier.weight(1f),
                leading = { Text(text = "🔥", fontSize = 26.sp) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(label = "Cards Played", value = "${summary.cardsPlayed}", modifier = Modifier.weight(1f))
            StatTile(label = "Success Rate", value = "${summary.successPercent}%", modifier = Modifier.weight(1f))
            StatTile(label = "Practice Time", value = summary.practiceTimeLabel, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "Quick Access",
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("🗂️", "Flashcards", onOpenFlashcards, Modifier.weight(1f))
            QuickTile("🧭", "Explore", onOpenExplore, Modifier.weight(1f))
            QuickTile("📝", "Grammar Rules", onOpenRules, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("📊", "Statistics", onOpenStats, Modifier.weight(1f))
            QuickTile("🔍", "Search", onOpenSearch, Modifier.weight(1f))
            QuickTile("👤", "Member", onOpenProfile, Modifier.weight(1f))
        }
    }
}

private fun streakLabel(days: Int): String = "$days ${if (days == 1) "day" else "days"}"

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Border, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(10.dp))
        }
        Column {
            Text(text = label, color = WortArtColors.TextSecondary, fontSize = 12.sp)
            Text(
                text = value,
                color = WortArtColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuickTile(emoji: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Text(
            text = label,
            color = WortArtColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
