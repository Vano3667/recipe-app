package com.example.homemenu.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Before Room upgrades an existing database, a byte copy of it is kept in filesDir/db_backups
 * (the last [KEEP] copies). If a migration ever goes wrong, the user's v1 data still exists on the device.
 */
object DatabaseSafety {
    private const val TAG = "HomeMenuDb"
    const val BACKUP_DIR = "db_backups"
    private const val KEEP = 5

    fun backupBeforeMigration(context: Context, name: String, targetVersion: Int): File? {
        val dbFile = context.getDatabasePath(name)
        if (!dbFile.exists()) return null
        return try {
            val version = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                // Fold the WAL into the main file so a single-file copy is complete.
                runCatching { db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() } }
                db.version
            }
            if (version <= 0 || version >= targetVersion) return null
            val dir = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
            val dest = File(dir, "${name.removeSuffix(".db")}_v${version}_${System.currentTimeMillis()}.db")
            dbFile.copyTo(dest, overwrite = false)
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(KEEP)?.forEach { it.delete() }
            Log.i(TAG, "Pre-migration backup v$version → ${dest.name}")
            dest
        } catch (e: Exception) {
            Log.w(TAG, "Pre-migration backup failed", e)
            null
        }
    }
}
