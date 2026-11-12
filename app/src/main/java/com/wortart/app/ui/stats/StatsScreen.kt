package com.wortart.app.ui.stats

import com.wortart.app.ui.components.AppIcon
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.domain.stats.Achievement
import com.wortart.app.domain.stats.ActivityHistory
import com.wortart.app.domain.stats.DayActivity
import com.wortart.app.domain.stats.StatsCalculator
import com.wortart.app.domain.stats.StatsSummary
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** Connects [StatsScreen] to its [StatsViewModel]. */
@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(summary = summary, onStartPractice = onStartPractice, modifier = modifier)
}

/**
 * The learner's progress: success rate ring, German Score, cards played, practice time,
 * correct answers, the daily streak with the last seven days, and achievements. Stateless.
 */
@Composable
fun StatsScreen(
    summary: StatsSummary,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = "Statistics",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontFamily = WortArtFonts.Serif,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Keep practicing every day",
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }

        if (summary.cardsPlayed == 0) {
            StartPracticeCard(onStartPractice = onStartPractice)
        }

        SuccessRing(
            percent = summary.successPercent,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                emoji = "⭐",
                label = "German Score",
                value = "${summary.germanScore}",
                tag = "statsGermanScore",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                emoji = "🃏",
                label = "Cards Played",
                value = "${summary.cardsPlayed}",
                tag = "statsCardsPlayed",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                emoji = "⏱️",
                label = "Practice Time",
                value = summary.practiceTimeLabel,
                tag = "statsPracticeTime",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                emoji = "✅",
                label = "Correct Answers",
                value = "${summary.correctAnswers}",
                tag = "statsCorrectAnswers",
                modifier = Modifier.weight(1f),
            )
        }

        StreakCard(
            days = summary.streakDays,
            practicedToday = summary.practicedToday,
            week = summary.weeklyActivity,
        )

        if (summary.achievements.isNotEmpty()) {
            AchievementsSection(summary.achievements)
        }

        Text(
            text = "Score: ${StatsCalculator.POINTS_PER_CORRECT_ANSWER} points per correct answer, " +
                "${StatsCalculator.POINTS_PER_STREAK_DAY} per day of your streak.",
            color = WortArtColors.TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StartPracticeCard(onStartPractice: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.5f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "No practice yet",
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Answer your first flashcard to see your progress here.",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
        GoldButton(text = "Start practicing", onClick = onStartPractice, modifier = Modifier.fillMaxWidth())
    }
}

/** A ring that fills clockwise with the success rate. */
@Composable
private fun SuccessRing(percent: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(180.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Success rate $percent percent" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = WortArtColors.Elevated,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (percent > 0) {
                drawArc(
                    color = WortArtColors.Gold,
                    startAngle = -90f,
                    sweepAngle = 360f * percent.coerceIn(0, 100) / 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                color = WortArtColors.TextPrimary,
                fontSize = 40.sp,
                fontFamily = WortArtFonts.Serif,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "Success Rate", color = WortArtColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StatTile(
    emoji: String,
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
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
        AppIcon(name = if (emoji == "⭐") "German Score" else "", emoji = emoji, size = 28.dp)
        Spacer(Modifier.size(10.dp))
        Column {
            Text(text = label, color = WortArtColors.TextSecondary, fontSize = 12.sp)
            Text(
                text = value,
                color = WortArtColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(tag),
            )
        }
    }
}

@Composable
private fun StreakCard(days: Int, practicedToday: Boolean, week: List<DayActivity>) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(name = "Daily Streak", emoji = "🔥", size = 28.dp)
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(text = "Daily Streak", color = WortArtColors.TextSecondary, fontSize = 12.sp)
                Text(
                    text = streakLabel(days),
                    color = WortArtColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = streakMessage(days, practicedToday),
                    color = WortArtColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
        if (week.isNotEmpty()) {
            WeekBars(week)
        }
    }
}

private fun streakLabel(days: Int): String = "$days ${if (days == 1) "day" else "days"}"

private fun streakMessage(days: Int, practicedToday: Boolean): String = when {
    practicedToday -> "You practiced today. Great job!"
    days > 0 -> "Practice today to keep your streak alive."
    else -> "Answer a flashcard to start your streak."
}

/** One bar per day of the last week. The height shows how many cards were answered. */
@Composable
private fun WeekBars(week: List<DayActivity>) {
    val maxAnswers = week.maxOf { it.answers }.coerceAtLeast(1)
    val maxBarHeight = 56.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEachIndexed { index, day ->
            val isToday = index == week.lastIndex
            val barHeight = if (day.answers == 0) 4.dp else maxBarHeight * (day.answers.toFloat() / maxAnswers)
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${ActivityHistory.dayName(day.epochDay)}: ${day.answers} ${if (day.answers == 1) "card" else "cards"}"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                day.answers == 0 -> WortArtColors.Elevated
                                isToday -> WortArtColors.Gold
                                else -> WortArtColors.GoldSecondary.copy(alpha = 0.7f)
                            },
                        ),
                )
                Text(
                    text = ActivityHistory.dayInitial(day.epochDay),
                    color = if (isToday) WortArtColors.Gold else WortArtColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Achievements",
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            achievements.forEach { AchievementChip(it) }
        }
    }
}

@Composable
private fun AchievementChip(achievement: Achievement) {
    val shape = RoundedCornerShape(16.dp)
    val state = if (achievement.earned) "earned" else "not earned yet"
    Column(
        modifier = Modifier
            .width(104.dp)
            .alpha(if (achievement.earned) 1f else 0.5f)
            .clip(shape)
            .background(if (achievement.earned) WortArtColors.Gold.copy(alpha = 0.12f) else WortArtColors.Surface)
            .border(
                width = 1.dp,
                color = if (achievement.earned) WortArtColors.Gold else WortArtColors.Border,
                shape = shape,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${achievement.title}, ${achievement.description}, $state"
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = if (achievement.earned) achievement.emoji else "🔒", fontSize = 24.sp)
        Text(
            text = achievement.title,
            color = if (achievement.earned) WortArtColors.Gold else WortArtColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
