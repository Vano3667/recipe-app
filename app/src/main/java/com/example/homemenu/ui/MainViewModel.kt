package com.example.homemenu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.files.PhotoStorage
import com.example.homemenu.data.repo.SettingsRepository
import com.example.homemenu.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

/** Everything the root theme needs; read from user_settings so it survives restarts and updates. */
data class AppearanceState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val themeId: String = "LEGACY",
    val customColors: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val theme: StateFlow<ThemeMode> = settings.observeTheme().stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val appearance: StateFlow<AppearanceState> = settings.observeSettings()
        .map { AppearanceState(ThemeMode.parse(it.themeMode), it.themeId, it.customColors) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceState())
}

/** Gives composables access to stored photo files without passing the storage around. */
@HiltViewModel
class PhotoViewModel @Inject constructor(private val storage: PhotoStorage) : ViewModel() {
    fun file(name: String?): File? = storage.fileFor(name)
}
