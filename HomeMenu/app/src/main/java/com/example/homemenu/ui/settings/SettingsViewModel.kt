package com.example.homemenu.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.files.BackupManager
import com.example.homemenu.data.repo.SettingsRepository
import com.example.homemenu.domain.backup.IncompatibleBackupException
import com.example.homemenu.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val backup: BackupManager,
) : ViewModel() {

    val theme: StateFlow<ThemeMode> = settings.observeTheme().stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    fun export(uri: Uri) = runTask {
        backup.exportTo(uri)
        "Резервная копия сохранена"
    }

    fun import(uri: Uri) = runTask {
        val r = backup.importFrom(uri)
        "Импортировано: рецептов — ${r.recipes}, фото — ${r.photos}, записей меню — ${r.menuEntries}"
    }

    fun messageShown() { _message.value = null }

    private fun runTask(block: suspend () -> String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _message.value = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IncompatibleBackupException) {
                e.message ?: "Несовместимая резервная копия"
            } catch (e: Exception) {
                "Ошибка: ${e.message ?: e::class.java.simpleName}"
            } finally {
                _busy.value = false
            }
        }
    }
}
