package com.wortart.app.ui.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.ui.components.FavoriteButton
import com.wortart.app.ui.components.LevelBadge
import com.wortart.app.ui.components.grammarTag
import com.wortart.app.ui.theme.WortArtColors

/** Connects [ExploreScreen] to its [ExploreViewModel] and handles the system Back button. */
@Composable
fun ExploreRoute(viewModel: ExploreViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    // Back first clears the search text, then leaves the open topic.
    BackHandler(enabled = query.isNotEmpty() || state.destination is ExploreDestination.Words) {
        if (query.isNotEmpty()) viewModel.clearQuery() else viewModel.closeTopic()
    }

    ExploreScreen(
        state = state,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onSelectLevel = viewModel::selectLevel,
        onOpenTopic = viewModel::openTopic,
        onCloseTopic = viewModel::closeTopic,
        onToggleFavorite = viewModel::toggleFavorite,
        modifier = modifier,
    )
}

/**
 * Explore tab: search field and level filter chips on top, then either the topic grid
 * or a word list (an open topic, or search results). Stateless.
 *
 * @param query the text of the search field, exactly as typed.
 */
@Composable
fun ExploreScreen(
    state: ExploreUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectLevel: (String?) -> Unit,
    onOpenTopic: (String?) -> Unit,
    onCloseTopic: () -> Unit,
    onToggleFavorite: (VocabEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        ExploreHeader(state = state, onBack = onCloseTopic)
        Spacer(Modifier.height(14.dp))
        SearchField(query = query, onQueryChange = onQueryChange)
        Spacer(Modifier.height(12.dp))
        LevelChips(selected = state.level, onSelect = onSelectLevel)
        Spacer(Modifier.height(16.dp))

        val showList = state.destination is ExploreDestination.Words || state.isSearching
        if (showList) {
            WordList(
                words = state.words,
                emptyMessage = if (state.isSearching) {
                    "No results for “${state.query}”."
                } else {
                    "No words found."
                },
                onToggleFavorite = onToggleFavorite,
            )
        } else {
            TopicGrid(
                topics = state.topics,
                totalWords = state.totalWords,
                onOpenTopic = onOpenTopic,
            )
        }
    }
}

private fun wordCountLabel(count: Int): String = "$count ${if (count == 1) "word" else "words"}"

@Composable
private fun ExploreHeader(state: ExploreUiState, onBack: () -> Unit) {
    when (val destination = state.destination) {
        ExploreDestination.Topics -> Column {
            Text(
                text = if (state.isSearching) "Search results" else "Explore",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (state.isSearching) {
                    wordCountLabel(state.words.size)
                } else {
                    "Browse words by topic and level"
                },
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }

        is ExploreDestination.Words -> Column {
            Text(
                text = "‹ Topics",
                color = WortArtColors.Gold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(vertical = 4.dp),
            )
            Text(
                text = destination.topic ?: "All words",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = wordCountLabel(state.words.size),
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("searchField"),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = {
            Text(text = "Search German, English or Bengali", color = WortArtColors.TextMuted)
        },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WortArtColors.TextPrimary,
            unfocusedTextColor = WortArtColors.TextPrimary,
            focusedContainerColor = WortArtColors.Surface,
            unfocusedContainerColor = WortArtColors.Surface,
            cursorColor = WortArtColors.Gold,
            focusedBorderColor = WortArtColors.Gold,
            unfocusedBorderColor = WortArtColors.Gold.copy(alpha = 0.4f),
            focusedLeadingIconColor = WortArtColors.Gold,
            unfocusedLeadingIconColor = WortArtColors.TextMuted,
            focusedTrailingIconColor = WortArtColors.Gold,
            unfocusedTrailingIconColor = WortArtColors.TextSecondary,
        ),
    )
}

@Composable
private fun LevelChips(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LevelChip(label = "All", selected = selected == null) { onSelect(null) }
        CefrLevels.ALL.forEach { level ->
            LevelChip(label = level, selected = selected == level) { onSelect(level) }
        }
    }
}

@Composable
private fun LevelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
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

@Composable
private fun TopicGrid(
    topics: List<TopicSummary>,
    totalWords: Int,
    onOpenTopic: (String?) -> Unit,
) {
    if (topics.isEmpty()) {
        Text(text = "Loading topics…", color = WortArtColors.TextSecondary, fontSize = 15.sp)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TopicCard(emoji = "📚", title = "All words", wordCount = totalWords) {
                onOpenTopic(null)
            }
        }
        items(topics, key = { it.name }) { topic ->
            TopicCard(
                emoji = topicEmoji(topic.name),
                title = topic.name,
                wordCount = topic.wordCount,
            ) {
                onOpenTopic(topic.name)
            }
        }
    }
}

@Composable
private fun TopicCard(emoji: String, title: String, wordCount: Int, onClick: () -> Unit) {
    val enabled = wordCount > 0
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.25f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = WortArtColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = wordCountLabel(wordCount),
            color = WortArtColors.TextMuted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun WordList(
    words: List<VocabEntity>,
    emptyMessage: String,
    onToggleFavorite: (VocabEntity) -> Unit,
) {
    if (words.isEmpty()) {
        Text(text = emptyMessage, color = WortArtColors.TextSecondary, fontSize = 15.sp)
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(words, key = { it.id }) { word ->
            WordRow(card = word, onToggleFavorite = { onToggleFavorite(word) })
        }
    }
}

@Composable
private fun WordRow(card: VocabEntity, onToggleFavorite: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.2f), shape)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.word,
                    color = WortArtColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(8.dp))
                LevelBadge(card.level)
            }
            Spacer(Modifier.height(2.dp))
            Text(text = card.grammarTag(), color = WortArtColors.Gold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = card.translationEn, color = WortArtColors.TextSecondary, fontSize = 15.sp)
            Text(text = card.translationBn, color = WortArtColors.GoldLight, fontSize = 15.sp)
        }
        FavoriteButton(isFavorite = card.isFavorite, onClick = onToggleFavorite)
    }
}
