package com.eclipse.webnovel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eclipse.webnovel.ui.nav.Routes
import com.eclipse.webnovel.ui.nav.TopDest
import com.eclipse.webnovel.ui.detail.DetailScreen
import com.eclipse.webnovel.ui.explore.ExploreScreen
import com.eclipse.webnovel.ui.library.LibraryScreen
import com.eclipse.webnovel.ui.reader.ReaderScreen
import com.eclipse.webnovel.ui.saved.SavedScreen
import com.eclipse.webnovel.ui.screens.SearchScreen
import com.eclipse.webnovel.ui.updates.UpdatesScreen
import com.eclipse.webnovel.ui.screens.SettingsScreen
import com.eclipse.webnovel.ui.theme.AppTheme
import com.eclipse.webnovel.ui.theme.liquidGlassSurface

@Composable
fun WebNovelApp(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == null || TopDest.items.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                GlassBottomBar(currentRoute) { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Explore.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopDest.Explore.route) {
                ExploreScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenNovel = { navController.navigate("${Routes.DETAIL}?url=${Uri.encode(it.url)}") },
                )
            }
            composable(TopDest.Search.route) { SearchScreen() }
            composable(TopDest.Library.route) {
                LibraryScreen(
                    onOpenNovel = { navController.navigate("${Routes.DETAIL}?url=${Uri.encode(it)}") },
                    onOpenUpdates = { navController.navigate(Routes.UPDATES) },
                )
            }
            composable(TopDest.Saved.route) { SavedScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.DETAIL}?url={url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { navController.navigate("${Routes.READER}?url=${Uri.encode(it.url)}") },
                )
            }
            composable(
                route = "${Routes.READER}?url={url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) {
                ReaderScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.UPDATES) {
                UpdatesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { navController.navigate("${Routes.READER}?url=${Uri.encode(it)}") },
                )
            }
        }
    }
}

@Composable
private fun GlassBottomBar(currentRoute: String?, onSelect: (TopDest) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .liquidGlassSurface(RoundedCornerShape(28.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopDest.items.forEach { dest ->
                NavBarItem(dest = dest, selected = currentRoute == dest.route) { onSelect(dest) }
            }
        }
    }
}

@Composable
private fun NavBarItem(dest: TopDest, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.primary else scheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(dest.icon, contentDescription = dest.label, tint = tint)
        Text(dest.label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
