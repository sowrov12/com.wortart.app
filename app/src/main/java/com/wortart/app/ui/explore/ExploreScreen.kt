package com.wortart.app.ui.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.domain.question.shortMeaning
import com.wortart.app.ui.components.EmptyState
import com.wortart.app.ui.components.FavoriteButton
import com.wortart.app.ui.components.LevelBadge
import com.wortart.app.ui.components.LevelFilterRow
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** Connects [ExploreScreen] to its [ExploreViewModel]. Back clears the filters first. */
@Composable
fun ExploreRoute(
    viewModel: ExploreViewModel,
    onOpenWord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    BackHandler(enabled = state.hasFilters || query.isNotEmpty()) {
        viewModel.clearFilters()
    }

    ExploreScreen(
        state = state,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onSelectLevel = viewModel::selectLevel,
        onSelectTopic = viewModel::selectTopic,
        onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
        onClearFilters = viewModel::clearFilters,
        onOpenWord = onOpenWord,
        onToggleFavorite = viewModel::toggleFavorite,
        modifier = modifier,
    )
}

/**
 * Explore tab: search, level chips, category cards and the list of matching words. All filters
 * combine and can be cleared. Stateless.
 *
 * @param query the text of the search field, exactly as typed.
 */
@Composable
fun ExploreScreen(
    state: ExploreUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectLevel: (String?) -> Unit,
    onSelectTopic: (String?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenWord: (String) -> Unit,
    onToggleFavorite: (VocabEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Explore",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontFamily = WortArtFonts.Serif,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            SearchRow(
                query = query,
                onQueryChange = onQueryChange,
                favoritesOnly = state.favoritesOnly,
                onToggleFavoritesOnly = onToggleFavoritesOnly,
            )
        }

        item { LevelFilterRow(selected = state.level, onSelect = onSelectLevel) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Categories",
                    color = WortArtColors.TextPrimary,
                    fontSize = 18.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.topics.isEmpty()) {
                    Text(text = "Loading categories…", color = WortArtColors.TextSecondary, fontSize = 14.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.topics, key = { it.name }) { topic ->
                            CategoryCard(
                                topic = topic,
                                selected = state.topic == topic.name,
                                onClick = { onSelectTopic(topic.name) },
                            )
                        }
                    }
                }
            }
        }

        item {
            ResultsHeader(
                title = if (state.hasFilters) "Results" else "Words",
                count = state.words.size,
                showClear = state.hasFilters && state.words.isNotEmpty(),
                onClear = onClearFilters,
            )
        }

        if (state.isLoaded && state.words.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🔍",
                    title = "No words found",
                    message = if (state.hasFilters) {
                        "Try another search or remove a filter."
                    } else {
                        "The vocabulary is still loading."
                    },
                    actionLabel = if (state.hasFilters) "Clear filters" else null,
                    onAction = if (state.hasFilters) onClearFilters else null,
                )
            }
        } else {
            items(state.words, key = { it.id }) { word ->
                WordRow(
                    card = word,
                    onClick = { onOpenWord(word.id) },
                    onToggleFavorite = { onToggleFavorite(word) },
                )
            }
        }
    }
}

@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    favoritesOnly: Boolean,
    onToggleFavoritesOnly: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .testTag("searchField"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(text = "Search words…", color = WortArtColors.TextMuted) },
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
                unfocusedBorderColor = WortArtColors.Border,
                focusedLeadingIconColor = WortArtColors.Gold,
                unfocusedLeadingIconColor = WortArtColors.TextMuted,
                focusedTrailingIconColor = WortArtColors.Gold,
                unfocusedTrailingIconColor = WortArtColors.TextSecondary,
            ),
        )
        Spacer(Modifier.size(10.dp))
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(shape)
                .background(if (favoritesOnly) WortArtColors.Gold.copy(alpha = 0.18f) else WortArtColors.Surface)
                .border(1.dp, if (favoritesOnly) WortArtColors.Gold else WortArtColors.Border, shape)
                .clickable(role = Role.Switch, onClick = onToggleFavoritesOnly),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Show favorites only",
                tint = WortArtColors.Gold,
            )
        }
    }
}

@Composable
private fun CategoryCard(topic: TopicSummary, selected: Boolean, onClick: () -> Unit) {
    val enabled = topic.wordCount > 0 || selected
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .width(100.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (selected) WortArtColors.Gold.copy(alpha = 0.14f) else WortArtColors.Surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) WortArtColors.Gold else WortArtColors.Border,
                shape = shape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = topicEmoji(topic.name), fontSize = 20.sp)
        }
        Text(
            text = topic.name,
            color = if (selected) WortArtColors.Gold else WortArtColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(text = "(${topic.wordCount})", color = WortArtColors.TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ResultsHeader(title: String, count: Int, showClear: Boolean, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$title ($count)",
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontFamily = WortArtFonts.Serif,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (showClear) {
            TextButton(onClick = onClear) {
                Text(text = "Clear filters", color = WortArtColors.Gold, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun WordRow(card: VocabEntity, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(WortArtColors.Gold.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = topicEmoji(card.topic), fontSize = 22.sp)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.word,
                color = WortArtColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = card.shortMeaning().replace(" / ", " | "),
                color = WortArtColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
        LevelBadge(card.level)
        FavoriteButton(isFavorite = card.isFavorite, onClick = onToggleFavorite)
    }
}
