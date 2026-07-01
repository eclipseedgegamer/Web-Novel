package com.eclipse.webnovel.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eclipse.webnovel.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Local app settings backed by Preferences DataStore. */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.fromName(prefs[themeKey])
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[themeKey] = theme.name }
    }
}
