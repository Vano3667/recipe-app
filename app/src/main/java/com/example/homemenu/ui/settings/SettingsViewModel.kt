package com.example.homemenu.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.files.BackupManager
import com.example.homemenu.data.files.StagedImport
import com.example.homemenu.data.repo.SettingsRepository
import com.example.homemenu.domain.backup.DuplicateMode
import com.example.homemenu.domain.backup.ImportReport
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

    /** A validated backup waiting for the user's confirmation (nothing written to the DB yet). */
    private val _staged = MutableStateFlow<StagedImport?>(null)
    val staged: StateFlow<StagedImport?> = _staged.asStateFlow()

    /** Result of the last confirmed import, shown as a report dialog. */
    private val _report = MutableStateFlow<ImportReport?>(null)
    val report: StateFlow<ImportReport?> = _report.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    fun exportZip(uri: Uri) = runTask {
        val r = backup.exportZip(uri)
        "Резервная копия ZIP сохранена: рецептов — ${r.recipes}, фото — ${r.photos}, блюд в меню — ${r.menuEntries}"
    }

    fun exportJson(uri: Uri) = runTask {
        val r = backup.exportJson(uri)
        "Экспорт JSON сохранён: рецептов — ${r.recipes}, фото — ${r.photos}, блюд в меню — ${r.menuEntries}"
    }

    /** Step 1 of import: read + validate + analyse the file, then show the preview. */
    fun stageImport(uri: Uri) = runTask {
        _staged.value?.let(backup::discard)
        _staged.value = backup.stage(uri)
        null
    }

    /** Step 2 of import: the user confirmed the preview. One transaction, then read-back verification. */
    fun confirmImport(mode: DuplicateMode) {
        val s = _staged.value ?: return
        _staged.value = null
        runTask {
            val r = backup.commit(s, mode)
            _report.value = r
            null
        }
    }

    fun cancelImport() {
        _staged.value?.let(backup::discard)
        _staged.value = null
    }

    fun reportShown() { _report.value = null }

    fun messageShown() { _message.value = null }

    override fun onCleared() {
        _staged.value?.let(backup::discard)
        super.onCleared()
    }

    private fun runTask(block: suspend () -> String?) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                block()?.let { _message.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IncompatibleBackupException) {
                _message.value = e.message ?: "Несовместимая резервная копия"
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message ?: e::class.java.simpleName}. Данные не изменены."
            } finally {
                _busy.value = false
            }
        }
    }
}
