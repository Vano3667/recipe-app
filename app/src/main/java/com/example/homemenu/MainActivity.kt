package com.example.homemenu

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import com.example.homemenu.ui.MainViewModel
import com.example.homemenu.ui.SharedTextInbox
import com.example.homemenu.ui.SharedTextIntent
import com.example.homemenu.ui.nav.AppRoot
import com.example.homemenu.ui.recipes.readTextFromUri
import com.example.homemenu.ui.theme.HomeMenuTheme
import com.example.homemenu.ui.theme.appIsDark
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Task 5: text shared from another app. Only on a fresh start, so a rotation does not reopen it.
        if (savedInstanceState == null) receiveSharedText(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveSharedText(intent)
    }

    /** Puts shared text (or the text of a shared TXT file) into [SharedTextInbox]; the Recipes screen shows it. */
    private fun receiveSharedText(intent: Intent?) {
        val text = SharedTextIntent.textOf(intent)
        if (text != null) {
            SharedTextInbox.offer(text)
            return
        }
        val uri = SharedTextIntent.streamOf(intent) ?: return
        lifecycleScope.launch {
            val read = readTextFromUri(this@MainActivity, uri)
            if (read != null) SharedTextInbox.offer(read)
            else Toast.makeText(this@MainActivity, "Не удалось прочитать присланный текст", Toast.LENGTH_LONG).show()
        }
    }
}
