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

    /**
     * Sets the identity to a stable ID derived from the Google account email.
     * Call this immediately after a successful Google Sign-In, before saving the profile.
     * If the email-based ID differs from the current one, the in-memory cache is updated
     * and persisted so all subsequent Firestore operations use the correct path.
     */
    fun setIdentityFromEmail(
        email: String,
        context: Context = FirebaseApp.getInstance().applicationContext
    ) {
        if (email.isBlank()) return
        val emailId = "google_${email.trim().lowercase()}"
        synchronized(this) {
            context.applicationContext
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ID, emailId)
                .apply()
            cachedUserId = emailId
        }
    }

    /**
     * Clears the persisted identity so the next login starts fresh.
     * A new device UUID is generated and stored — it will be replaced by
     * setIdentityFromEmail() as soon as the user signs in with Google.
     */
    fun resetIdentity(context: Context = FirebaseApp.getInstance().applicationContext) {
        synchronized(this) {
            val newId = "device_${UUID.randomUUID()}"
            context.applicationContext
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ID, newId)
                .apply()
            cachedUserId = newId
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
