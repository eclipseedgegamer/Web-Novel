package com.eclipse.webnovel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eclipse.webnovel.data.settings.SettingsRepository
import com.eclipse.webnovel.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)

    val theme: StateFlow<AppTheme> = settings.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppTheme.Default,
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settings.setTheme(theme) }
    }
}
