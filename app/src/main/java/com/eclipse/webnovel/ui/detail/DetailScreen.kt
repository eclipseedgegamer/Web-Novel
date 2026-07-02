package com.eclipse.webnovel.ui.detail

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eclipse.webnovel.data.db.ReadingStateEntity
import com.eclipse.webnovel.data.download.DownloadRepository
import com.eclipse.webnovel.data.library.LibraryRepository
import com.eclipse.webnovel.data.model.ChapterRef
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.source.SourceRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val detail: NovelDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val library = LibraryRepository(app)
    private val downloads = DownloadRepository(app)
    private val novelUrl: String = requireNotNull(savedStateHandle.get<String>("url")) { "missing url arg" }

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val inLibrary: StateFlow<Boolean> = library.isInLibrary(novelUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val readingState: StateFlow<ReadingStateEntity?> = library.observeState(novelUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { load() }

    fun load() {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { SourceRegistry.sourceFor(novelUrl).detail(novelUrl) }.fold(
                onSuccess = { DetailUiState.Success(it) },
                onFailure = { DetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun toggleLibrary() {
        val detail = (_state.value as? DetailUiState.Success)?.detail ?: return
        viewModelScope.launch {
            if (inLibrary.value) library.remove(detail.summary.url) else library.add(detail)
        }
    }

    fun recordReading(chapter: ChapterRef, index: Int, total: Int) {
        viewModelScope.launch { library.recordReading(novelUrl, chapter, index, total) }
    }

    fun download() {
        downloads.enqueue(novelUrl)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onOpenChapter: (ChapterRef) -> Unit,
    viewModel: DetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val inLibrary by viewModel.inLibrary.collectAsState()
    val readingState by viewModel.readingState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::download) {
                        Icon(Icons.Outlined.Download, contentDescription = "Download")
                    }
                    IconButton(onClick = viewModel::toggleLibrary) {
                        Icon(
                            imageVector = if (inLibrary) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (inLibrary) "Remove from library" else "Add to library",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is DetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is DetailUiState.Error -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = viewModel::load) { Text("Retry") }
                }
                is DetailUiState.Success -> DetailContent(
                    detail = s.detail,
                    readingState = readingState,
                    onOpenChapter = { chapter, index ->
                        viewModel.recordReading(chapter, index, s.detail.chapters.size)
                        onOpenChapter(chapter)
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: NovelDetail,
    readingState: ReadingStateEntity?,
    onOpenChapter: (ChapterRef, Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = detail.summary.coverUrl,
                    contentDescription = detail.summary.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(150.dp).height(220.dp).clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    text = detail.summary.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
                val subtitle = listOfNotNull(detail.author, detail.status).joinToString("  ·  ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (detail.chapters.isNotEmpty()) {
                    val targetIndex = readingState
                        ?.let { rs -> detail.chapters.indexOfFirst { it.url == rs.lastChapterUrl } }
                        ?.takeIf { it >= 0 } ?: 0
                    val label = if (readingState != null) "Continue · Ch ${targetIndex + 1}" else "Start reading"
                    Button(
                        onClick = { onOpenChapter(detail.chapters[targetIndex], targetIndex) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text(label) }
                }
            }
        }
        item {
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        item {
            Text(
                text = "CHAPTERS · ${detail.chapters.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
        }
        itemsIndexed(detail.chapters) { index, chapter ->
            ChapterRow(chapter) { onOpenChapter(chapter, index) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun ChapterRow(chapter: ChapterRef, onClick: () -> Unit) {
    Text(
        text = chapter.title,
        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Default),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
