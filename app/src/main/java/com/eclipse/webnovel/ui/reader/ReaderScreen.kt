package com.eclipse.webnovel.ui.reader

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipse.webnovel.data.download.DownloadRepository
import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.settings.ProgressRepository
import com.eclipse.webnovel.data.settings.SettingsRepository
import com.eclipse.webnovel.data.source.RoyalRoadSource
import com.eclipse.webnovel.ui.theme.liquidGlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Success(val content: ChapterContent) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

class ReaderViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val source = RoyalRoadSource()
    private val progress = ProgressRepository(app)
    private val settings = SettingsRepository(app)
    private val downloads = DownloadRepository(app)
    private val chapterUrl: String = requireNotNull(savedStateHandle.get<String>("url")) { "missing url arg" }

    val fontSize: StateFlow<Int> = settings.readerFontSp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_READER_FONT_SP)

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    var restoreParagraph: Int = 0
        private set

    init { load() }

    fun load() {
        _state.value = ReaderUiState.Loading
        viewModelScope.launch {
            restoreParagraph = progress.get(chapterUrl)?.paragraphIndex ?: 0
            val cached = downloads.cachedChapter(chapterUrl)
            _state.value = if (cached != null) {
                ReaderUiState.Success(
                    ChapterContent(
                        title = cached.title,
                        paragraphs = cached.content.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() },
                    ),
                )
            } else {
                runCatching { source.chapter(chapterUrl) }.fold(
                    onSuccess = { ReaderUiState.Success(it) },
                    onFailure = { ReaderUiState.Error(it.message ?: "Failed to load chapter") },
                )
            }
        }
    }

    fun saveProgress(paragraphIndex: Int) {
        viewModelScope.launch { progress.save(chapterUrl, paragraphIndex, 0) }
    }

    fun setFontSize(sp: Int) {
        viewModelScope.launch { settings.setReaderFontSp(sp.coerceIn(12, 28)) }
    }
}

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    when (val s = state) {
        is ReaderUiState.Loading -> CenterBox { CircularProgressIndicator() }
        is ReaderUiState.Error -> CenterBox { ErrorRetry(s.message, viewModel::load) }
        is ReaderUiState.Success -> ReaderContent(
            content = s.content,
            fontSize = fontSize,
            restoreParagraph = viewModel.restoreParagraph,
            onSaveProgress = viewModel::saveProgress,
            onFontSize = viewModel::setFontSize,
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    content: ChapterContent,
    fontSize: Int,
    restoreParagraph: Int,
    onSaveProgress: (Int) -> Unit,
    onFontSize: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(false) }
    var showAaSheet by remember { mutableStateOf(false) }
    val paragraphs = content.paragraphs

    // Restore once to the saved paragraph (index is font-independent). Title is item 0.
    LaunchedEffect(content) {
        if (restoreParagraph > 0) {
            listState.scrollToItem((restoreParagraph + 1).coerceAtMost(paragraphs.size))
        }
    }
    // Persist the top visible paragraph, debounced via collectLatest + delay.
    LaunchedEffect(content) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { itemIndex ->
                delay(400)
                onSaveProgress((itemIndex - 1).coerceAtLeast(0))
            }
    }

    val progressPct = if (paragraphs.isEmpty()) 0
    else (listState.firstVisibleItemIndex.coerceIn(0, paragraphs.size) * 100 / paragraphs.size)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val pagePx = with(LocalDensity.current) { (maxHeight * 0.85f).toPx() }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        when {
                            offset.x < size.width / 3f -> scope.launch { listState.animateScrollBy(-pagePx) }
                            offset.x > size.width * 2f / 3f -> scope.launch { listState.animateScrollBy(pagePx) }
                            else -> chromeVisible = !chromeVisible
                        }
                    }
                },
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 76.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            itemsIndexed(paragraphs) { _, paragraph ->
                Text(
                    text = paragraph,
                    fontFamily = FontFamily.Serif,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.6f).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .liquidGlassSurface(RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .liquidGlassSurface(RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$progressPct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAaSheet = true }) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showAaSheet) {
        ModalBottomSheet(onDismissRequest = { showAaSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Text size", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(onClick = { onFontSize(fontSize - 1) }) { Text("A-") }
                    Text("$fontSize sp", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { onFontSize(fontSize + 1) }) { Text("A+") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ErrorRetry(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text("Retry") }
    }
}
