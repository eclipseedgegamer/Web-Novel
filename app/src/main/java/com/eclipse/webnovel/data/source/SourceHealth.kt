package com.eclipse.webnovel.data.source

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory record of each source's last fetch outcome, shown in the Source Manager. */
object SourceHealth {

    private val _state = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val state: StateFlow<Map<String, Boolean>> = _state.asStateFlow()

    fun record(sourceId: String, ok: Boolean) {
        _state.value = _state.value.toMutableMap().apply { put(sourceId, ok) }
    }
}
