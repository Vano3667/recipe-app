package com.example.homemenu

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.homemenu.ui.MainViewModel
import com.example.homemenu.ui.nav.AppRoot
import com.example.homemenu.ui.theme.HomeMenuTheme
import com.example.homemenu.ui.theme.appIsDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by vm.appearance.collectAsState()
            val dark = appIsDark(appearance.mode, appearance.themeId, appearance.customColors, isSystemInDarkTheme())
            // Status/navigation bar icons follow the app theme, not only the system one.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            val windowSize = calculateWindowSizeClass(this)
            HomeMenuTheme(appearance.mode, appearance.themeId, appearance.customColors) {
                AppRoot(windowSize.widthSizeClass)
            }
        }
    }
}
