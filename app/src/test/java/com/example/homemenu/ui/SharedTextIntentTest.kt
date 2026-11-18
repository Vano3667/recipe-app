package com.example.homemenu.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Task 5: plain text shared from other apps (ACTION_SEND text/plain). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SharedTextIntentTest {

    @After fun clearInbox() { SharedTextInbox.take() }

    private fun send(type: String?, text: String? = null, subject: String? = null) = Intent(Intent.ACTION_SEND).apply {
        if (type != null) setType(type)
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
        if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
    }

    @Test fun plainTextIsRead() {
        val msg = "Блины\nМука 200 г\nМолоко 500 мл"
        assertEquals(msg, SharedTextIntent.textOf(send("text/plain", msg)))
    }

    @Test fun subjectBecomesFirstLine() {
        assertEquals("Блины\nМука 200 г", SharedTextIntent.textOf(send("text/plain", "Мука 200 г", subject = "Блины")))
    }

    @Test fun subjectNotRepeatedWhenBodyStartsWithIt() {
        assertEquals("Блины\nМука 200 г", SharedTextIntent.textOf(send("text/plain", "Блины\nМука 200 г", subject = "Блины")))
    }

    @Test fun subjectOnly() {
        assertEquals("Блины", SharedTextIntent.textOf(send("text/plain", subject = "  Блины ")))
    }

    @Test fun blankOrMissingTextGivesNull() {
        assertNull(SharedTextIntent.textOf(send("text/plain", "   ")))
        assertNull(SharedTextIntent.textOf(send("text/plain")))
    }

    @Test fun otherActionsAndTypesAreIgnored() {
        assertNull(SharedTextIntent.textOf(null))
        assertNull(SharedTextIntent.textOf(Intent(Intent.ACTION_MAIN)))
        assertNull(SharedTextIntent.textOf(Intent(Intent.ACTION_VIEW).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "x")))
        assertNull(SharedTextIntent.textOf(send("image/jpeg", "x")))
        assertNull(SharedTextIntent.textOf(send(null, "x")))
        assertFalse(SharedTextIntent.isTextShare(send("application/zip")))
        assertTrue(SharedTextIntent.isTextShare(send("text/plain")))
    }

    @Test fun sharedTextFileUriIsReturned() {
        val uri = Uri.parse("content://com.example.notes/recipes/1.txt")
        val intent = send("text/plain").putExtra(Intent.EXTRA_STREAM, uri)
        assertNull(SharedTextIntent.textOf(intent))
        assertEquals(uri, SharedTextIntent.streamOf(intent))
        assertNull(SharedTextIntent.streamOf(send("image/png").putExtra(Intent.EXTRA_STREAM, uri)))
    }

    @Test fun inboxHandsTextOutOnce() {
        SharedTextInbox.offer("Борщ")
        assertEquals("Борщ", SharedTextInbox.pending.value)
        assertEquals("Борщ", SharedTextInbox.take())
        assertNull(SharedTextInbox.take())
        assertNull(SharedTextInbox.pending.value)
    }

    @Test fun inboxIgnoresBlankAndCapsLength() {
        SharedTextInbox.offer("  ")
        SharedTextInbox.offer(null)
        assertNull(SharedTextInbox.take())
        SharedTextInbox.offer("а".repeat(SharedTextInbox.MAX_CHARS + 10))
        assertEquals(SharedTextInbox.MAX_CHARS, SharedTextInbox.take()?.length)
    }
}
