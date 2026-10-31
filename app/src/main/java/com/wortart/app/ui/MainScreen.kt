package com.wortart.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.wortart.app.ui.theme.WortArtColors

/** Bottom navigation tabs. Flashcard practice opens from Home as a full-screen session. */
enum class MainTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Explore("Explore", Icons.Filled.Search),
    Rules("Rules", Icons.Filled.Info),
    Stats("Stats", Icons.Filled.Star),
    Profile("Profile", Icons.Filled.Person),
}

/**
 * App shell with the dark bottom navigation bar. The selected tab is owned by the caller so
 * that screens (for example the Home tiles) can switch tabs. Each tab keeps its saveable
 * state while another tab is open.
 */
@Composable
fun MainScreen(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    home: @Composable () -> Unit,
    explore: @Composable () -> Unit,
    rules: @Composable () -> Unit,
    stats: @Composable () -> Unit,
    profile: @Composable () -> Unit,
) {
    val stateHolder: SaveableStateHolder = rememberSaveableStateHolder()

    Scaffold(
        containerColor = WortArtColors.Background,
        bottomBar = {
            NavigationBar(containerColor = WortArtColors.SurfaceDark) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WortArtColors.Gold,
                            selectedTextColor = WortArtColors.Gold,
                            indicatorColor = Color.Transparent,
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
            stateHolder.SaveableStateProvider(key = selectedTab.name) {
                when (selectedTab) {
                    MainTab.Home -> home()
                    MainTab.Explore -> explore()
                    MainTab.Rules -> rules()
                    MainTab.Stats -> stats()
                    MainTab.Profile -> profile()
                }
            }
        }
    }
}
