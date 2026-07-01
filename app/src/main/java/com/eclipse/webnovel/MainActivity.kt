package com.eclipse.webnovel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.eclipse.webnovel.ui.MainViewModel
import com.eclipse.webnovel.ui.WebNovelApp
import com.eclipse.webnovel.ui.theme.WebNovelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.theme.collectAsState()
            WebNovelTheme(appTheme = theme) {
                WebNovelApp(
                    currentTheme = theme,
                    onThemeChange = viewModel::setTheme,
                )
            }
        }
    }
}
