package com.example.homemenu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.repo.SettingsRepository
import com.example.homemenu.ui.theme.AppThemeId
import com.example.homemenu.ui.theme.CustomAppearance
import com.example.homemenu.ui.theme.CustomRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceUi(
    val themeId: AppThemeId = AppThemeId.LEGACY,
    val custom: CustomAppearance = CustomAppearance(),
)

/**
 * Settings > Appearance. Every change is written to user_settings right away, so the theme applies
 * immediately (MainActivity observes the same row) and survives restarts and app updates.
 * Custom colours are kept when another theme is chosen, so switching back restores them.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppearanceUi> = settings.observeSettings()
        .map { AppearanceUi(AppThemeId.parse(it.themeId), CustomAppearance.decode(it.customColors)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceUi())

    fun select(id: AppThemeId) {
        viewModelScope.launch {
            val current = settings.get()
            settings.setAppearance(id.name, current.customColors)
        }
    }

    fun setCustomColor(role: CustomRole, color: Long) {
        viewModelScope.launch {
            val custom = CustomAppearance.decode(settings.get().customColors).withColor(role, color)
            settings.setAppearance(AppThemeId.CUSTOM.name, custom.encode())
        }
    }

    /** Starts the custom theme from another preset; earlier overrides are dropped. */
    fun setCustomBase(base: AppThemeId) {
        if (base !in AppThemeId.presets) return
        viewModelScope.launch {
            settings.setAppearance(AppThemeId.CUSTOM.name, CustomAppearance(base = base).encode())
        }
    }

    /** Clears the user's colours but stays on the custom theme with the same base. */
    fun resetCustom() {
        viewModelScope.launch {
            val base = CustomAppearance.decode(settings.get().customColors).base
            settings.setAppearance(AppThemeId.CUSTOM.name, CustomAppearance(base = base).encode())
        }
    }

    /** Back to the default (original) theme; saved custom colours are kept for later. */
    fun resetToDefault() {
        viewModelScope.launch {
            val current = settings.get()
            settings.setAppearance(AppThemeId.LEGACY.name, current.customColors)
        }
    }
}
