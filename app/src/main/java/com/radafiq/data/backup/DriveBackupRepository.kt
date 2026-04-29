package com.radafiq.data.backup

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

class DriveBackupRepository {
    fun uploadBackup(accessToken: String, backupJson: String): Result<Unit> {
        return runCatching {
            val existingFileId = findBackupFileId(accessToken)
            val boundary = "Radafiq-boundary"
            // "parents" must only be set on POST (create), not on PATCH (update)
            val metadata = JSONObject().apply {
                put("name", BACKUP_FILE_NAME)
                put("mimeType", BACKUP_MIME_TYPE)
                if (existingFileId.isNullOrBlank()) {
                    put("parents", JSONArray().put("appDataFolder"))
                }
            }

            val requestBody = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString()).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: ").append(BACKUP_MIME_TYPE).append("\r\n\r\n")
                append(backupJson).append("\r\n")
                append("--").append(boundary).append("--")
            }

            val endpoint = if (existingFileId.isNullOrBlank()) {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
            }

            val connection = openConnection(
                url = endpoint,
                accessToken = accessToken,
                method = if (existingFileId.isNullOrBlank()) "POST" else "PATCH",
                contentType = "multipart/related; boundary=$boundary"
            )

            try {
                connection.outputStream.use { output ->
                    OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                        writer.write(requestBody)
                        writer.flush()
                    }
                }
                ensureSuccess(connection)
            } finally {
                connection.disconnect()
            }
        }
    }

    fun downloadLatestBackup(accessToken: String): Result<String> {
        return runCatching {
            val fileId = findBackupFileId(accessToken)
                ?: error("No Google Drive backup was found for this account.")

            val connection = openConnection(
                url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media",
                accessToken = accessToken,
                method = "GET"
            )

            try {
                ensureSuccess(connection)
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun findBackupFileId(accessToken: String): String? {
        val query = URLEncoder.encode("name='$BACKUP_FILE_NAME' and trashed=false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,modifiedTime)&q=$query"
        val connection = openConnection(url = url, accessToken = accessToken, method = "GET")

        try {
            ensureSuccess(connection)
            val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val files = JSONObject(response).optJSONArray("files") ?: JSONArray()
            if (files.length() == 0) return null

            // Find the latest file
            var latestId: String? = null
            var latestModified = ""
            val allIds = mutableListOf<String>()
            for (index in 0 until files.length()) {
                val file = files.getJSONObject(index)
                val id = file.optString("id")
                val modifiedTime = file.optString("modifiedTime")
                allIds.add(id)
                if (latestId == null || modifiedTime > latestModified) {
                    latestId = id
                    latestModified = modifiedTime
                }
            }

            // Delete any duplicate files (keep only the latest)
            allIds.filter { it != latestId }.forEach { duplicateId ->
                runCatching {
                    val deleteConn = openConnection(
                        url = "https://www.googleapis.com/drive/v3/files/$duplicateId",
                        accessToken = accessToken,
                        method = "DELETE"
                    )
                    try {
                        deleteConn.responseCode // trigger the request
                    } finally {
                        deleteConn.disconnect()
                    }
                }
            }

            return latestId
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        url: String,
        accessToken: String,
        method: String,
        contentType: String? = null
    ): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000   // 20s to establish connection
            readTimeout    = 30_000   // 30s to read response
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            if (contentType != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
    }

    private fun ensureSuccess(connection: HttpURLConnection) {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                ?: "Request failed with HTTP $responseCode"
            // Strip any potential token leakage from error messages
            val safeError = errorBody.take(200).replace(Regex("Bearer [A-Za-z0-9._\\-]+"), "Bearer [REDACTED]")
            error("HTTP $responseCode: $safeError")
        }
    }

    companion object {
        const val BACKUP_FILE_NAME = "Radafiq_backup.json"
        const val BACKUP_MIME_TYPE = "application/json"
    }
}
