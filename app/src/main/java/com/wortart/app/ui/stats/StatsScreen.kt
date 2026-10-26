package com.wortart.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.domain.stats.StatsCalculator
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.theme.WortArtColors

/** Connects [StatsScreen] to its [StatsViewModel]. */
@Composable
fun StatsRoute(viewModel: StatsViewModel, modifier: Modifier = Modifier) {
    val summary by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(summary = summary, modifier = modifier)
}

/** The learner's progress: German Score, daily streak, cards played, success rate, practice time. */
@Composable
fun StatsScreen(summary: StatsSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text(
                text = "Your Progress",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Keep practicing every day",
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }

        ScoreCard(score = summary.germanScore)
        StreakCard(days = summary.streakDays, practicedToday = summary.practicedToday)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Cards played",
                value = "${summary.cardsPlayed}",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Success rate",
                value = "${summary.successPercent}%",
                modifier = Modifier.weight(1f),
                progress = summary.successPercent / 100f,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Practice time",
                value = summary.practiceTimeLabel,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Perfect pronunciations",
                value = "${summary.perfectPronunciations}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScoreCard(score: Int) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.5f), shape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "GERMAN SCORE",
            color = WortArtColors.TextMuted,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = "$score",
            color = WortArtColors.Gold,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${StatsCalculator.POINTS_PER_CORRECT_ANSWER} points per correct answer · " +
                "${StatsCalculator.POINTS_PER_PERFECT_PRONUNCIATION} per perfect pronunciation · " +
                "${StatsCalculator.POINTS_PER_STREAK_DAY} per streak day",
            color = WortArtColors.TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StreakCard(days: Int, practicedToday: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.25f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🔥", fontSize = 24.sp)
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(text = "DAILY STREAK", color = WortArtColors.TextMuted, fontSize = 12.sp, letterSpacing = 2.sp)
            Text(
                text = streakLabel(days),
                color = WortArtColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = streakMessage(days, practicedToday),
                color = WortArtColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

private fun streakLabel(days: Int): String = "$days ${if (days == 1) "day" else "days"}"

private fun streakMessage(days: Int, practicedToday: Boolean): String = when {
    practicedToday -> "You practiced today. Great job!"
    days > 0 -> "Practice today to keep your streak alive."
    else -> "Answer a flashcard to start your streak."
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.25f), shape)
            .padding(16.dp),
    ) {
        Text(
            text = value,
            color = WortArtColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = WortArtColors.TextMuted, fontSize = 13.sp)
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            ProgressBar(fraction = progress.coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(WortArtColors.SurfaceDark),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(WortArtColors.Gold),
        )
    }
}
