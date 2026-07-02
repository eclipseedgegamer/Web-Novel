package com.eclipse.webnovel.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eclipse.webnovel.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Local app settings backed by Preferences DataStore. */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")
    private val readerFontKey = intPreferencesKey("reader_font_sp")

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.fromName(prefs[themeKey])
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[themeKey] = theme.name }
    }

    val readerFontSp: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[readerFontKey] ?: DEFAULT_READER_FONT_SP
    }

    suspend fun setReaderFontSp(sp: Int) {
        context.dataStore.edit { prefs -> prefs[readerFontKey] = sp }
    }

    private val disabledSourcesKey = stringSetPreferencesKey("disabled_sources")

    val disabledSources: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[disabledSourcesKey] ?: emptySet()
    }

    suspend fun setSourceEnabled(id: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[disabledSourcesKey] ?: emptySet()
            prefs[disabledSourcesKey] = if (enabled) current - id else current + id
        }
    }

    companion object {
        const val DEFAULT_READER_FONT_SP = 18
    }
}
