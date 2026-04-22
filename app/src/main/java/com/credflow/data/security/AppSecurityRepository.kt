package com.credflow.data.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSecurityState(
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasPasscode: Boolean = false,
    val isUnlocked: Boolean = true
)

class AppSecurityRepository(context: Context) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppSecurityState> = _state.asStateFlow()

    fun setPasscode(passcode: String) {
        val normalized = passcode.trim()
        if (normalized.length < 4) return

        preferences.edit()
            .putString(KEY_PASSCODE_HASH, hashPasscode(normalized))
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()

        _state.value = _state.value.copy(
            hasPasscode = true,
            lockEnabled = true,
            isUnlocked = true
        )
    }

    fun clearPasscode() {
        preferences.edit()
            .remove(KEY_PASSCODE_HASH)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()

        _state.value = AppSecurityState(
            lockEnabled = false,
            biometricEnabled = false,
            hasPasscode = false,
            isUnlocked = true
        )
    }

    fun setLockEnabled(enabled: Boolean) {
        if (enabled && !_state.value.hasPasscode) return

        preferences.edit()
            .putBoolean(KEY_LOCK_ENABLED, enabled)
            .apply()

        _state.value = _state.value.copy(
            lockEnabled = enabled,
            isUnlocked = if (enabled) _state.value.isUnlocked else true
        )
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (enabled && !_state.value.hasPasscode) return

        preferences.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()

        _state.value = _state.value.copy(biometricEnabled = enabled)
    }

    fun verifyPasscode(passcode: String): Boolean {
        val storedHash = preferences.getString(KEY_PASSCODE_HASH, null).orEmpty()
        val matches = storedHash.isNotBlank() && storedHash == hashPasscode(passcode.trim())
        if (matches) {
            unlock()
        }
        return matches
    }

    fun unlock() {
        _state.value = _state.value.copy(isUnlocked = true)
    }

    fun lock() {
        if (_state.value.lockEnabled && _state.value.hasPasscode) {
            _state.value = _state.value.copy(isUnlocked = false)
        }
    }

    fun exportSettings(): Map<String, Any> {
        return mapOf(
            KEY_LOCK_ENABLED to _state.value.lockEnabled,
            KEY_BIOMETRIC_ENABLED to _state.value.biometricEnabled
        )
    }

    fun restoreSettings(data: Map<String, Any?>) {
        val lockEnabled = data[KEY_LOCK_ENABLED] as? Boolean ?: false
        val biometricEnabled = data[KEY_BIOMETRIC_ENABLED] as? Boolean ?: false

        preferences.edit()
            .putBoolean(KEY_LOCK_ENABLED, lockEnabled && _state.value.hasPasscode)
            .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled && _state.value.hasPasscode)
            .apply()

        _state.value = _state.value.copy(
            lockEnabled = lockEnabled && _state.value.hasPasscode,
            biometricEnabled = biometricEnabled && _state.value.hasPasscode,
            isUnlocked = if (lockEnabled && _state.value.hasPasscode) false else true
        )
    }

    private fun loadState(): AppSecurityState {
        val hasPasscode = !preferences.getString(KEY_PASSCODE_HASH, null).isNullOrBlank()
        val lockEnabled = preferences.getBoolean(KEY_LOCK_ENABLED, false) && hasPasscode
        val biometricEnabled = preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false) && hasPasscode
        return AppSecurityState(
            lockEnabled = lockEnabled,
            biometricEnabled = biometricEnabled,
            hasPasscode = hasPasscode,
            isUnlocked = !lockEnabled
        )
    }

    private fun hashPasscode(passcode: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(passcode.toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
    private companion object {
        const val PREFERENCES_NAME = "credflow_security"
        const val KEY_PASSCODE_HASH = "passcode_hash"
        const val KEY_LOCK_ENABLED = "lock_enabled"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
