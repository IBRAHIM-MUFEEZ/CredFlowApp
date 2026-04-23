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
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to read the selected backup file.")

                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        }
    }
}
