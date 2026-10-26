package com.wortart.app.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.domain.rules.RuleBlock
import com.wortart.app.domain.rules.RuleSection
import com.wortart.app.ui.theme.WortArtColors

/** Connects [GrammarRulesScreen] to its [RulesViewModel]. */
@Composable
fun GrammarRulesRoute(viewModel: RulesViewModel, modifier: Modifier = Modifier) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    GrammarRulesScreen(sections = sections, modifier = modifier)
}

/**
 * Quick-reference grammar rules as expandable cards. One card is open at a time,
 * which keeps the screen short on a phone. Stateless apart from the open card.
 */
@Composable
fun GrammarRulesScreen(sections: List<RuleSection>, modifier: Modifier = Modifier) {
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Grammar Rules",
                    color = WortArtColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Quick reference for A1–B2 learners",
                    color = WortArtColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }
        items(sections, key = { it.id }) { section ->
            RuleCard(
                section = section,
                expanded = expandedId == section.id,
                onToggle = {
                    expandedId = if (expandedId == section.id) null else section.id
                },
            )
        }
    }
}

@Composable
private fun RuleCard(section: RuleSection, expanded: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(
                width = 1.dp,
                color = WortArtColors.Gold.copy(alpha = if (expanded) 0.5f else 0.25f),
                shape = shape,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Collapse" else "Expand",
                    onClick = onToggle,
                )
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    color = WortArtColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = section.subtitle, color = WortArtColors.Gold, fontSize = 13.sp)
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = WortArtColors.Gold,
                modifier = Modifier.rotate(chevronRotation),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                section.blocks.forEach { block -> RuleBlockView(block) }
            }
        }
    }
}

@Composable
private fun RuleBlockView(block: RuleBlock) {
    when (block) {
        is RuleBlock.Paragraph -> Text(
            text = block.text,
            color = WortArtColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        is RuleBlock.BulletList -> BulletListView(block.items)
        is RuleBlock.Table -> RuleTableView(block)
        is RuleBlock.ExampleList -> ExampleListView(block)
    }
}

@Composable
private fun BulletListView(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row {
                Text(text = "•", color = WortArtColors.Gold, fontSize = 15.sp)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = item,
                    color = WortArtColors.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RuleTableView(table: RuleBlock.Table) {
    val cellWidth = when {
        table.headers.size >= 5 -> 92.dp
        table.headers.size == 4 -> 104.dp
        else -> 124.dp
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        table.title?.let { title ->
            Text(
                text = title,
                color = WortArtColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Wide tables scroll sideways inside their own container.
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .horizontalScroll(rememberScrollState()),
        ) {
            RuleTableRow(
                cells = table.headers,
                cellWidth = cellWidth,
                isHeader = true,
                background = WortArtColors.Gold.copy(alpha = 0.16f),
            )
            table.rows.forEachIndexed { index, row ->
                RuleTableRow(
                    cells = row,
                    cellWidth = cellWidth,
                    isHeader = false,
                    background = if (index % 2 == 0) WortArtColors.SurfaceDark else Color.Transparent,
                )
            }
        }
    }
}

@Composable
private fun RuleTableRow(cells: List<String>, cellWidth: Dp, isHeader: Boolean, background: Color) {
    Row(modifier = Modifier.background(background)) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = cell,
                color = when {
                    isHeader -> WortArtColors.Gold
                    index == 0 -> WortArtColors.TextPrimary
                    else -> WortArtColors.TextSecondary
                },
                fontSize = 14.sp,
                fontWeight = if (isHeader || index == 0) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .width(cellWidth)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ExampleListView(block: RuleBlock.ExampleList) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        block.title?.let { title ->
            Text(
                text = title,
                color = WortArtColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        block.examples.forEach { example ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WortArtColors.SurfaceDark, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = example.de, color = WortArtColors.TextPrimary, fontSize = 16.sp)
                Text(text = example.en, color = WortArtColors.TextSecondary, fontSize = 14.sp)
                Text(text = example.bn, color = WortArtColors.GoldLight, fontSize = 14.sp)
            }
        }
    }
}
