package com.eclipse.webnovel.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Font-independent reading position: paragraph index (+ intra-paragraph char offset). */
data class ReadingPosition(val paragraphIndex: Int, val charOffset: Int)

private val Context.progressDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "reading_progress")

/** Per-chapter reading progress, keyed by chapter URL. Room replaces this in Phase 2. */
class ProgressRepository(private val context: Context) {

    suspend fun get(chapterUrl: String): ReadingPosition? {
        val raw = context.progressDataStore.data.map { it[key(chapterUrl)] }.first() ?: return null
        val parts = raw.split(":")
        return ReadingPosition(
            paragraphIndex = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            charOffset = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        )
    }

    suspend fun save(chapterUrl: String, paragraphIndex: Int, charOffset: Int) {
        context.progressDataStore.edit { it[key(chapterUrl)] = "$paragraphIndex:$charOffset" }
    }

    private fun key(chapterUrl: String) = stringPreferencesKey("pos:$chapterUrl")
}
