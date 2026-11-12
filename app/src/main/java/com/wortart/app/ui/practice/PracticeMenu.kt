package com.wortart.app.ui.practice

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.sp
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.domain.question.QuestionType
import com.wortart.app.ui.theme.WortArtColors

/** What the three-dot menu of the flashcards shows and does. */
data class PracticeMenuConfig(
    val levels: Set<String> = emptySet(),
    val readAloud: Boolean = true,
    val reminders: Boolean = true,
    val showBengali: Boolean = true,
    val onToggleLevel: (String) -> Unit = {},
    val onClearLevels: () -> Unit = {},
    val onReadAloud: (Boolean) -> Unit = {},
    val onReminders: (Boolean) -> Unit = {},
    val onShowBengali: (Boolean) -> Unit = {},
    val onSkip: () -> Unit = {},
    val onOpenCategories: () -> Unit = {},
    val onSelectSet: (QuestionType?) -> Unit = {},
)

private enum class MenuPage { MAIN, LEVEL, LANGUAGE }

/** The three-dot menu: Level, Read flashcards, Reminders, Skip word, Language. */
@Composable
fun PracticeMenu(config: PracticeMenuConfig) {
    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(MenuPage.MAIN) }

    Box {
        IconButton(onClick = {
            page = MenuPage.MAIN
            expanded = true
        }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = WortArtColors.TextPrimary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = WortArtColors.Surface,
        ) {
            when (page) {
                MenuPage.MAIN -> {
                    MenuRow("🎓", "Level", onClick = { page = MenuPage.LEVEL }) { Chevron() }
                    MenuRow("🔊", "Read flashcards", onClick = { config.onReadAloud(!config.readAloud) }) {
                        MenuSwitch(config.readAloud) { config.onReadAloud(it) }
                    }
                    MenuRow("🔔", "Reminders", onClick = { config.onReminders(!config.reminders) }) {
                        MenuSwitch(config.reminders) { config.onReminders(it) }
                    }
                    MenuRow("⏭", "Skip word", onClick = {
                        expanded = false
                        config.onSkip()
                    }) {}
                    MenuRow("🌐", "Language", onClick = { page = MenuPage.LANGUAGE }) { Chevron() }
                }

                MenuPage.LEVEL -> {
                    BackRow("Level") { page = MenuPage.MAIN }
                    listOf(CefrLevels.A1, CefrLevels.A2, CefrLevels.B1, CefrLevels.B2).forEach { level ->
                        CheckRow(level, level in config.levels) { config.onToggleLevel(level) }
                    }
                }

                MenuPage.LANGUAGE -> {
                    BackRow("Language") { page = MenuPage.MAIN }
                    CheckRow("English", true) {}
                    CheckRow("বাংলা", config.showBengali) { config.onShowBengali(!config.showBengali) }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = label, color = WortArtColors.TextPrimary, fontSize = 16.sp) },
        leadingIcon = { Text(text = emoji, fontSize = 18.sp) },
        trailingIcon = trailing,
        onClick = onClick,
    )
}

@Composable
private fun BackRow(title: String, onBack: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = title, color = WortArtColors.Gold, fontSize = 16.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WortArtColors.Gold,
            )
        },
        onClick = onBack,
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = label, color = WortArtColors.TextPrimary, fontSize = 16.sp) },
        trailingIcon = {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = WortArtColors.Gold,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = WortArtColors.TextSecondary,
    )
}

@Composable
private fun MenuSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = WortArtColors.OnGold,
            checkedTrackColor = WortArtColors.Gold,
        ),
    )
}
