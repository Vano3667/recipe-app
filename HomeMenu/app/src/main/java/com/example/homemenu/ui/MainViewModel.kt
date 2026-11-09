package com.example.homemenu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.files.PhotoStorage
import com.example.homemenu.data.repo.SettingsRepository
import com.example.homemenu.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val theme: StateFlow<ThemeMode> = settings.observeTheme().stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}

/** Gives composables access to stored photo files without passing the storage around. */
@HiltViewModel
class PhotoViewModel @Inject constructor(private val storage: PhotoStorage) : ViewModel() {
    fun file(name: String?): File? = storage.fileFor(name)
}
