package com.radafiq.data.backup

import com.radafiq.data.models.BackupRecord
import com.radafiq.data.models.FirestoreBackupPayload
import org.json.JSONArray
import org.json.JSONObject

object BackupJsonSerializer {
    fun toJson(payload: FirestoreBackupPayload): String {
        return JSONObject().apply {
            put("version", payload.version)
            put("exportedAt", payload.exportedAt)
            put("profile", mapToJson(payload.profile))
            put("settings", mapToJson(payload.settings))
            put("customers", recordsToJson(payload.customers))
            put("accounts", recordsToJson(payload.accounts))
            put("transactions", recordsToJson(payload.transactions))
            put("payments", recordsToJson(payload.payments))
        }.toString()
    }

    fun fromJson(json: String): FirestoreBackupPayload {
        val root = JSONObject(json)
        return FirestoreBackupPayload(
            version = root.optInt("version", 1),
            exportedAt = root.optString("exportedAt"),
            profile = jsonToMap(root.optJSONObject("profile")),
            settings = jsonToMap(root.optJSONObject("settings")),
            customers = jsonToRecords(root.optJSONArray("customers")),
            accounts = jsonToRecords(root.optJSONArray("accounts")),
            transactions = jsonToRecords(root.optJSONArray("transactions")),
            payments = jsonToRecords(root.optJSONArray("payments"))
        )
    }

    private fun recordsToJson(records: List<BackupRecord>): JSONArray {
        return JSONArray().apply {
            records.forEach { record ->
                put(
                    JSONObject().apply {
                        put("id", record.id)
                        put("fields", mapToJson(record.fields))
                    }
                )
            }
        }
    }

    private fun jsonToRecords(array: JSONArray?): List<BackupRecord> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    BackupRecord(
                        id = item.optString("id"),
                        fields = jsonToMap(item.optJSONObject("fields"))
                    )
                )
            }
        }
    }

    private fun mapToJson(map: Map<String, Any?>): JSONObject {
        return JSONObject().apply {
            map.forEach { (key, value) ->
                put(key, anyToJsonValue(value))
            }
        }
    }

    private fun jsonToMap(jsonObject: JSONObject?): Map<String, Any?> {
        if (jsonObject == null) return emptyMap()
        val result = linkedMapOf<String, Any?>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = jsonToAnyValue(jsonObject.get(key))
        }
        return result
    }

    private fun anyToJsonValue(value: Any?): Any? {
        return when (value) {
            null -> JSONObject.NULL
            is Map<*, *> -> mapToJson(value.entries.associate { it.key.toString() to it.value })
            is Iterable<*> -> JSONArray().apply {
                value.forEach { put(anyToJsonValue(it)) }
            }
            else -> value
        }
    }

    private fun jsonToAnyValue(value: Any?): Any? {
        return when (value) {
            JSONObject.NULL -> null
            is JSONObject -> jsonToMap(value)
            is JSONArray -> buildList {
                for (index in 0 until value.length()) {
                    add(jsonToAnyValue(value.get(index)))
                }
            }
            else -> value
        }
    }
}
