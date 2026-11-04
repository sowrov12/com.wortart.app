package com.wortart.app.ui.worddetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.question.shortMeaning
import com.wortart.app.domain.rules.RuleBlock
import com.wortart.app.domain.word.DetailSection
import com.wortart.app.domain.word.WordDetailsContent
import com.wortart.app.ui.components.AudioButton
import com.wortart.app.ui.components.ExampleBox
import com.wortart.app.ui.components.FavoriteButton
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.components.LevelBadge
import com.wortart.app.ui.components.OutlineButton
import com.wortart.app.ui.components.WortArtTopBar
import com.wortart.app.ui.rules.RuleTableView
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** Connects [WordDetailsScreen] to its [WordDetailsViewModel] and handles the Back button. */
@Composable
fun WordDetailsRoute(
    viewModel: WordDetailsViewModel,
    isSpeaking: Boolean,
    onSpeak: (wordId: String, word: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    WordDetailsScreen(
        state = state,
        isSpeaking = isSpeaking,
        onBack = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onSpeak = onSpeak,
        modifier = modifier,
    )
}

/** Everything about one word: forms, conjugation, example, favorite and audio. Stateless. */
@Composable
fun WordDetailsScreen(
    state: WordDetailsUiState,
    isSpeaking: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSpeak: (wordId: String, word: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = state.card
    val content = state.content

    Column(modifier = modifier.fillMaxSize()) {
        WortArtTopBar(
            title = "Word Details",
            onBack = onBack,
            trailing = {
                if (card != null) {
                    FavoriteButton(isFavorite = card.isFavorite, onClick = onToggleFavorite)
                }
            },
        )

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WortArtColors.Gold)
            }

            card == null || content == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "This word could not be found.",
                    color = WortArtColors.TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                OutlineButton(text = "Go back", onClick = onBack)
            }

            else -> {
                WordBody(
                    card = card,
                    content = content,
                    isSpeaking = isSpeaking,
                    onSpeak = onSpeak,
                    modifier = Modifier.weight(1f),
                )
                BottomActions(
                    card = card,
                    onToggleFavorite = onToggleFavorite,
                    onSpeak = { onSpeak(card.id, card.word) },
                )
            }
        }
    }
}

@Composable
private fun WordBody(
    card: VocabEntity,
    content: WordDetailsContent,
    isSpeaking: Boolean,
    onSpeak: (wordId: String, word: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.word,
                    color = WortArtColors.TextPrimary,
                    fontSize = 36.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                AudioButton(
                    word = card.word,
                    isSpeaking = isSpeaking,
                    onClick = { onSpeak(card.id, card.word) },
                )
            }
            Text(
                text = card.shortMeaning(),
                color = WortArtColors.TextSecondary,
                fontSize = 17.sp,
            )
        }

        InfoRow(categoryLabel = content.categoryLabel, level = card.level)

        content.sections.forEach { section ->
            when (section) {
                is DetailSection.Table -> SectionCard(title = section.title) {
                    RuleTableView(RuleBlock.Table(title = null, headers = section.headers, rows = section.rows))
                }
                is DetailSection.Facts -> SectionCard(title = section.title) {
                    FactsList(section.items)
                }
            }
        }

        if (!content.hasExtraDetails) {
            Text(
                text = "More details for this word are not available yet.",
                color = WortArtColors.TextMuted,
                fontSize = 14.sp,
            )
        }

        SectionCard(title = "Example Sentence") { ExampleBox(card) }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun InfoRow(categoryLabel: String, level: String) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = categoryLabel,
            color = WortArtColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        LevelBadge(level)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = WortArtColors.TextPrimary,
            fontSize = 17.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun FactsList(items: List<Pair<String, String>>) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.SurfaceDark)
            .border(1.dp, WortArtColors.Border, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    color = WortArtColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    color = WortArtColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun BottomActions(card: VocabEntity, onToggleFavorite: () -> Unit, onSpeak: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlineButton(
            text = if (card.isFavorite) "Remove from Favorites" else "Add to Favorites",
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f),
        )
        GoldButton(text = "Play Audio", onClick = onSpeak, modifier = Modifier.weight(1f))
    }
}
