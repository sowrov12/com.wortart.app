package com.wortart.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.domain.question.PracticeSets
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.ui.components.PillChip
import com.wortart.app.ui.theme.WortArtColors

/**
 * The flashcard filter: which card set to practice and which levels. Levels can be mixed freely;
 * with no level selected every level is used. Changes apply at once.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeFilterSheet(
    group: String?,
    type: QuestionType?,
    levels: Set<String>,
    onSelectSet: (QuestionType?) -> Unit,
    onToggleLevel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WortArtColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetTitle("Card set")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PillChip(
                    label = PracticeSets.label(null),
                    selected = type == null,
                    onClick = { onSelectSet(null) },
                )
                PracticeSets.forGroup(group).forEach { set ->
                    PillChip(
                        label = PracticeSets.label(set),
                        selected = type == set,
                        onClick = { onSelectSet(set) },
                    )
                }
            }

            SheetTitle("Level (you can pick more than one)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                listOf(CefrLevels.A1, CefrLevels.A2, CefrLevels.B1, CefrLevels.B2).forEach { level ->
                    PillChip(
                        label = level,
                        selected = level in levels,
                        onClick = { onToggleLevel(level) },
                    )
                }
            }
            Text(
                text = "No level selected means all levels.",
                color = WortArtColors.TextMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        color = WortArtColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp),
    )
}
