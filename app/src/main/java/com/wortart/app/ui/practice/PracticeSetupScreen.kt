package com.wortart.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.domain.question.PracticeSets
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.ui.components.GoldButton
import com.wortart.app.ui.components.PillChip
import com.wortart.app.ui.components.WortArtTopBar
import com.wortart.app.ui.theme.WortArtColors

/**
 * Setup before a flashcard session: category, card set and one or more levels.
 * Stateless; the choices live in [PracticeUiState.config].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeSetupScreen(
    state: PracticeUiState,
    onBack: () -> Unit,
    onSelectGroup: (String?) -> Unit,
    onSelectSet: (QuestionType?) -> Unit,
    onToggleLevel: (String) -> Unit,
    onClearLevels: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = state.config
    val sets = PracticeSets.forGroup(config.group)

    Column(modifier = modifier.fillMaxSize()) {
        WortArtTopBar(title = "Flashcards", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PillChip(label = "All", selected = config.group == null, onClick = { onSelectGroup(null) })
                state.presentGroups.forEach { group ->
                    PillChip(
                        label = group,
                        selected = config.group == group,
                        onClick = { onSelectGroup(group) },
                    )
                }
            }

            SectionTitle("Card set")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PillChip(
                    label = PracticeSets.label(null),
                    selected = config.type == null,
                    onClick = { onSelectSet(null) },
                )
                sets.forEach { type ->
                    PillChip(
                        label = PracticeSets.label(type),
                        selected = config.type == type,
                        onClick = { onSelectSet(type) },
                    )
                }
            }

            SectionTitle("Level (you can pick more than one)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PillChip(label = "All", selected = config.levels.isEmpty(), onClick = onClearLevels)
                CefrLevels.ALL.forEach { level ->
                    PillChip(
                        label = level,
                        selected = level in config.levels,
                        onClick = { onToggleLevel(level) },
                    )
                }
            }
        }

        GoldButton(
            text = "Start",
            enabled = state.availableCount > 0,
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = WortArtColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp),
    )
}
