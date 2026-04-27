package com.radafiq.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileBackupRepository(
    private val context: Context
) {
    suspend fun writeBackup(
        uri: Uri,
        backupJson: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val outputStream = context.contentResolver.openOutputStream(uri, "w")
                    ?: error("Unable to open the selected backup file.")

                outputStream.bufferedWriter().use { writer ->
                    writer.write(backupJson)
                    writer.flush()
                }
            }
        }
    }

    suspend fun readBackup(uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                // Check file size first via ContentResolver before reading
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val fileSize = cursor?.use {
                    if (it.moveToFirst()) {
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex >= 0) it.getLong(sizeIndex) else -1L
                    } else {
                        -1L
                    }
                } ?: -1L

                if (fileSize > 50 * 1024 * 1024) {
                    error("Backup file is too large (max 50 MB).")
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to read the selected backup file.")

                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        }
    }
}
