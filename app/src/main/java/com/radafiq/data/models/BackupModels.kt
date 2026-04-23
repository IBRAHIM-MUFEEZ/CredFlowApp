package com.radafiq.data.models

data class BackupRecord(
    val id: String,
    val fields: Map<String, Any?>
)

data class FirestoreBackupPayload(
    val version: Int = 1,
    val exportedAt: String,
    val profile: Map<String, Any?> = emptyMap(),
    val settings: Map<String, Any?> = emptyMap(),
    val customers: List<BackupRecord> = emptyList(),
    val accounts: List<BackupRecord> = emptyList(),
    val transactions: List<BackupRecord> = emptyList(),
    val payments: List<BackupRecord> = emptyList()
)
