package com.eclipse.webnovel.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eclipse.webnovel.data.search.DedupedNovel
import com.eclipse.webnovel.data.search.SearchRepository
import com.eclipse.webnovel.data.settings.SettingsRepository
import com.eclipse.webnovel.data.source.SourceRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val novels: List<DedupedNovel>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SearchRepository()
    private val settings = SettingsRepository(app)
    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = SearchUiState.Loading
        viewModelScope.launch {
            val disabled = settings.disabledSources.first()
            val sources = SourceRegistry.all.filter { it.id !in disabled }
            _state.value = runCatching { repository.search(query, sources) }.fold(
                onSuccess = { SearchUiState.Results(it) },
                onFailure = { SearchUiState.Error(it.message ?: "Search failed") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenNovel: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Search", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Titles across sources…") },
                trailingIcon = {
                    IconButton(onClick = { viewModel.search(query) }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(Modifier.fillMaxSize()) {
                when (val s = state) {
                    is SearchUiState.Idle -> Hint("Search RoyalRoad and NovelFire at once.")
                    is SearchUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is SearchUiState.Error -> Hint(s.message)
                    is SearchUiState.Results -> if (s.novels.isEmpty()) {
                        Hint("No results.")
                    } else {
                        LazyColumn(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(s.novels) { novel ->
                                ResultRow(novel) { onOpenNovel(novel.hits.first().url) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(novel: DedupedNovel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = novel.coverUrl,
            contentDescription = novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(56.dp).height(80.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(Modifier.padding(top = 4.dp)) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = novel.hits.joinToString(" · ") { it.sourceName },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
    )
}
