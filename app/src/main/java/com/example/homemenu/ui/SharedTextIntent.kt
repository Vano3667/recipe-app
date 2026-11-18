package com.example.homemenu.ui

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Task 5: reads plain text that another app shares to Home Menu (ACTION_SEND, text/plain).
 * Either the text itself (EXTRA_TEXT, with EXTRA_SUBJECT as a first line when it adds something)
 * or a shared text file (EXTRA_STREAM). Other actions and non-text types are ignored.
 */
object SharedTextIntent {
    fun isTextShare(intent: Intent?): Boolean =
        intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true

    fun textOf(intent: Intent?): String? {
        val i = intent?.takeIf { isTextShare(it) } ?: return null
        val body = i.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        val subject = i.getStringExtra(Intent.EXTRA_SUBJECT)
        return combine(subject, body)
    }

    fun streamOf(intent: Intent?): Uri? {
        val i = intent?.takeIf { isTextShare(it) } ?: return null
        return IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
    }

    /** Subject first (e-mail apps put the recipe title there), unless the body already starts with it. */
    fun combine(subject: String?, body: String?): String? {
        val b = body?.trim().orEmpty()
        val s = subject?.trim().orEmpty()
        return when {
            b.isEmpty() && s.isEmpty() -> null
            b.isEmpty() -> s
            s.isEmpty() || b.startsWith(s) -> b
            else -> "$s\n$b"
        }
    }
}

/**
 * Holds shared text between MainActivity (which receives the intent) and the Recipes screen
 * (which opens the text import dialog pre-filled). The text is handed out once; nothing is saved
 * until the user confirms the preview in the dialog.
 */
object SharedTextInbox {
    const val MAX_CHARS = 50_000

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(text: String?) {
        val t = text?.takeIf { it.isNotBlank() } ?: return
        _pending.value = t.take(MAX_CHARS)
    }

    /** Returns the pending text and clears it, so the dialog opens only once per share. */
    fun take(): String? = _pending.getAndUpdate { null }
}
