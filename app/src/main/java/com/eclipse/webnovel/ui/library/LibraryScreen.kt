package com.eclipse.webnovel.ui.library

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eclipse.webnovel.data.db.LibraryNovelEntity
import com.eclipse.webnovel.data.db.ReadingStateEntity
import com.eclipse.webnovel.data.library.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LibraryItem(val novel: LibraryNovelEntity, val state: ReadingStateEntity?)

enum class LibraryTab(val label: String) { ALL("All"), READING("Reading"), COMPLETED("Completed") }

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = LibraryRepository(app)

    val library: StateFlow<List<LibraryItem>> = combine(
        repository.observeLibrary(),
        repository.observeAllStates(),
    ) { novels, states ->
        val byUrl = states.associateBy { it.novelUrl }
        novels.map { LibraryItem(it, byUrl[it.novelUrl]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unseenUpdates: StateFlow<Int> = repository.observeUnseenCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenNovel: (String) -> Unit,
    onOpenUpdates: () -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val items by viewModel.library.collectAsState()
    val unseen by viewModel.unseenUpdates.collectAsState()
    var tab by rememberSaveable { mutableStateOf(LibraryTab.ALL) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onOpenUpdates) {
                        BadgedBox(badge = { if (unseen > 0) Badge() }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Updates")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabChips(selected = tab, onSelect = { tab = it })
            val filtered = when (tab) {
                LibraryTab.ALL -> items
                LibraryTab.READING -> items.filter { it.state != null }
                LibraryTab.COMPLETED -> items.filter { it.novel.status.equals("COMPLETED", ignoreCase = true) }
            }
            Box(Modifier.fillMaxSize()) {
                if (filtered.isEmpty()) {
                    Text(
                        text = if (items.isEmpty()) {
                            "No saved novels yet.\nBookmark one from its page."
                        } else {
                            "Nothing in ${tab.label}."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filtered) { item -> LibraryRow(item) { onOpenNovel(item.novel.novelUrl) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChips(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryTab.entries.forEach { entry ->
            val isSelected = entry == selected
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    )
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = item.novel.coverUrl,
            contentDescription = item.novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(60.dp).height(86.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(Modifier.padding(top = 4.dp)) {
            Text(
                text = item.novel.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.novel.author.isNullOrBlank()) {
                Text(
                    text = item.novel.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val state = item.state
            if (state != null && state.totalChapters > 0) {
                val fraction = ((state.chapterIndex + 1).toFloat() / state.totalChapters).coerceIn(0f, 1f)
                Text(
                    text = "Ch ${state.chapterIndex + 1} / ${state.totalChapters}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
