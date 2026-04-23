package com.radafiq.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import java.util.UUID

object LocalIdentityRepository {
    private const val PREFERENCES_NAME = "Radafiq_local_identity"
    private const val KEY_USER_ID = "local_user_id"

    @Volatile
    private var cachedUserId: String? = null

    fun userId(context: Context = FirebaseApp.getInstance().applicationContext): String {
        cachedUserId?.let { return it }

        return synchronized(this) {
            cachedUserId ?: loadOrCreateUserId(context.applicationContext).also { cachedUserId = it }
        }
    }

    private fun loadOrCreateUserId(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val existingUserId = preferences.getString(KEY_USER_ID, null)
        if (!existingUserId.isNullOrBlank()) {
            return existingUserId
        }

        val generatedUserId = "device_${UUID.randomUUID()}"
        preferences.edit()
            .putString(KEY_USER_ID, generatedUserId)
            .apply()
        return generatedUserId
    }
}
