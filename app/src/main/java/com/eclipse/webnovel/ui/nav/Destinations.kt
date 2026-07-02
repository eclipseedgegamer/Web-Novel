package com.eclipse.webnovel.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level bottom-navigation destinations. */
sealed class TopDest(val route: String, val label: String, val icon: ImageVector) {
    data object Explore : TopDest("explore", "Explore", Icons.Outlined.Explore)
    data object Search : TopDest("search", "Search", Icons.Outlined.Search)
    data object Library : TopDest("library", "Library", Icons.Outlined.CollectionsBookmark)
    data object Saved : TopDest("saved", "Saved", Icons.Outlined.Download)

    companion object {
        val items = listOf(Explore, Search, Library, Saved)
    }
}

object Routes {
    const val SETTINGS = "settings"
    const val DETAIL = "detail"
    const val READER = "reader"
    const val UPDATES = "updates"
    const val SOURCES = "sources"
}
