package com.wortart.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.wortart.app.ui.theme.WortArtColors

/** Bottom navigation tabs. Stats (Task 4.1) is added later. */
enum class MainTab(val label: String, val icon: ImageVector) {
    Practice("Practice", Icons.Filled.PlayArrow),
    Explore("Explore", Icons.Filled.Search),
    Rules("Rules", Icons.Filled.Info),
}

/**
 * App shell with the dark-gold bottom navigation bar. Each tab keeps its
 * remembered (saveable) state while another tab is open.
 */
@Composable
fun MainScreen(
    practice: @Composable () -> Unit,
    explore: @Composable () -> Unit,
    rules: @Composable () -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder: SaveableStateHolder = rememberSaveableStateHolder()

    Scaffold(
        containerColor = WortArtColors.Background,
        bottomBar = {
            NavigationBar(containerColor = WortArtColors.Surface) {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WortArtColors.OnGold,
                            selectedTextColor = WortArtColors.Gold,
                            indicatorColor = WortArtColors.Gold,
                            unselectedIconColor = WortArtColors.TextMuted,
                            unselectedTextColor = WortArtColors.TextMuted,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            stateHolder.SaveableStateProvider(key = selectedIndex) {
                when (MainTab.entries[selectedIndex]) {
                    MainTab.Practice -> practice()
                    MainTab.Explore -> explore()
                    MainTab.Rules -> rules()
                }
            }
        }
    }
}
