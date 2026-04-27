package com.radafiq.data.security

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
    val hasRecoveryQuestion: Boolean = false,
    val recoveryQuestion: String = "",
    val isUnlocked: Boolean = true
)

class AppSecurityRepository(context: Context) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppSecurityState> = _state.asStateFlow()

    fun setPasscode(
        passcode: String,
        recoveryQuestion: String,
        recoveryAnswer: String
    ) {
        val normalized = passcode.trim()
        val normalizedQuestion = recoveryQuestion.trim()
        val normalizedAnswer = normalizeRecoveryAnswer(recoveryAnswer)
        if (
            normalized.length < 4 ||
            normalizedQuestion.isBlank() ||
            normalizedAnswer.isBlank()
        ) {
            return
        }

        persistSecurity(
            passcode = normalized,
            recoveryQuestion = normalizedQuestion,
            recoveryAnswer = normalizedAnswer,
            lockEnabled = true,
            biometricEnabled = _state.value.biometricEnabled,
            isUnlocked = true
        )
    }

    fun updatePasscode(
        currentPasscode: String,
        newPasscode: String,
        recoveryQuestion: String,
        recoveryAnswer: String
    ): Boolean {
        if (!matchesPasscode(currentPasscode)) return false

        val normalized = newPasscode.trim()
        val normalizedQuestion = recoveryQuestion.trim()
        val normalizedAnswer = normalizeRecoveryAnswer(recoveryAnswer)
        if (
            normalized.length < 4 ||
            normalizedQuestion.isBlank() ||
            normalizedAnswer.isBlank()
        ) {
            return false
        }

        persistSecurity(
            passcode = normalized,
            recoveryQuestion = normalizedQuestion,
            recoveryAnswer = normalizedAnswer,
            lockEnabled = _state.value.lockEnabled,
            biometricEnabled = _state.value.biometricEnabled,
            isUnlocked = true
        )
        return true
    }

    fun clearPasscode() {
        preferences.edit()
            .remove(KEY_PASSCODE_HASH)
            .remove(KEY_RECOVERY_QUESTION)
            .remove(KEY_RECOVERY_ANSWER_HASH)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()

        _state.value = AppSecurityState(
            lockEnabled = false,
            biometricEnabled = false,
            hasPasscode = false,
            hasRecoveryQuestion = false,
            recoveryQuestion = "",
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
        val matches = matchesPasscode(passcode)
        if (matches) {
            unlock()
        }
        return matches
    }

    fun resetPasscodeWithRecovery(
        recoveryAnswer: String,
        newPasscode: String
    ): Boolean {
        val currentRecoveryQuestion = _state.value.recoveryQuestion
        if (
            !_state.value.hasRecoveryQuestion ||
            !matchesRecoveryAnswer(recoveryAnswer) ||
            newPasscode.trim().length < 4 ||
            currentRecoveryQuestion.isBlank()
        ) {
            return false
        }

        persistSecurity(
            passcode = newPasscode.trim(),
            recoveryQuestion = currentRecoveryQuestion,
            recoveryAnswer = normalizeRecoveryAnswer(recoveryAnswer),
            lockEnabled = true,
            biometricEnabled = _state.value.biometricEnabled,
            isUnlocked = true
        )
        return true
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
        val passcodeHash = preferences.getString(KEY_PASSCODE_HASH, null).orEmpty()
        val passcodeSalt = preferences.getString(KEY_PASSCODE_SALT, null).orEmpty()
        val recoveryQuestion = preferences.getString(KEY_RECOVERY_QUESTION, null).orEmpty()
        val recoveryAnswerHash = preferences.getString(KEY_RECOVERY_ANSWER_HASH, null).orEmpty()
        return mapOf(
            KEY_LOCK_ENABLED to _state.value.lockEnabled,
            KEY_BIOMETRIC_ENABLED to _state.value.biometricEnabled,
            KEY_PASSCODE_HASH to passcodeHash,
            KEY_PASSCODE_SALT to passcodeSalt,
            KEY_RECOVERY_QUESTION to recoveryQuestion,
            KEY_RECOVERY_ANSWER_HASH to recoveryAnswerHash
        )
    }

    fun restoreSettings(data: Map<String, Any?>) {
        val lockEnabled = data[KEY_LOCK_ENABLED] as? Boolean ?: false
        val biometricEnabled = data[KEY_BIOMETRIC_ENABLED] as? Boolean ?: false
        val passcodeHash = (data[KEY_PASSCODE_HASH] as? String).orEmpty()
        val passcodeSalt = (data[KEY_PASSCODE_SALT] as? String).orEmpty()
        val recoveryQuestion = (data[KEY_RECOVERY_QUESTION] as? String).orEmpty()
        val recoveryAnswerHash = (data[KEY_RECOVERY_ANSWER_HASH] as? String).orEmpty()

        val hasPasscode = passcodeHash.isNotBlank()
        val hasRecovery = recoveryQuestion.isNotBlank() && recoveryAnswerHash.isNotBlank()

        preferences.edit().apply {
            if (hasPasscode) putString(KEY_PASSCODE_HASH, passcodeHash)
            if (passcodeSalt.isNotBlank()) putString(KEY_PASSCODE_SALT, passcodeSalt)
            if (recoveryQuestion.isNotBlank()) putString(KEY_RECOVERY_QUESTION, recoveryQuestion)
            if (recoveryAnswerHash.isNotBlank()) putString(KEY_RECOVERY_ANSWER_HASH, recoveryAnswerHash)
            putBoolean(KEY_LOCK_ENABLED, lockEnabled && hasPasscode)
            putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled && hasPasscode)
            apply()
        }

        _state.value = AppSecurityState(
            lockEnabled = lockEnabled && hasPasscode,
            biometricEnabled = biometricEnabled && hasPasscode,
            hasPasscode = hasPasscode,
            hasRecoveryQuestion = hasRecovery,
            recoveryQuestion = recoveryQuestion,
            isUnlocked = true
        )
    }

    private fun loadState(): AppSecurityState {
        val hasPasscode = !preferences.getString(KEY_PASSCODE_HASH, null).isNullOrBlank()
        val recoveryQuestion = preferences.getString(KEY_RECOVERY_QUESTION, null).orEmpty()
        val hasRecoveryQuestion = recoveryQuestion.isNotBlank() &&
            !preferences.getString(KEY_RECOVERY_ANSWER_HASH, null).isNullOrBlank()
        val lockEnabled = preferences.getBoolean(KEY_LOCK_ENABLED, false) && hasPasscode
        val biometricEnabled = preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false) && hasPasscode
        return AppSecurityState(
            lockEnabled = lockEnabled,
            biometricEnabled = biometricEnabled,
            hasPasscode = hasPasscode,
            hasRecoveryQuestion = hasRecoveryQuestion,
            recoveryQuestion = recoveryQuestion,
            isUnlocked = !lockEnabled
        )
    }

    private fun persistSecurity(
        passcode: String,
        recoveryQuestion: String,
        recoveryAnswer: String,
        lockEnabled: Boolean,
        biometricEnabled: Boolean,
        isUnlocked: Boolean
    ) {
        preferences.edit()
            .putString(KEY_PASSCODE_HASH, hashPasscode(passcode))
            .putString(KEY_RECOVERY_QUESTION, recoveryQuestion)
            .putString(KEY_RECOVERY_ANSWER_HASH, hashRecoveryAnswer(recoveryAnswer))
            .putBoolean(KEY_LOCK_ENABLED, lockEnabled)
            .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
            .apply()

        _state.value = AppSecurityState(
            lockEnabled = lockEnabled,
            biometricEnabled = biometricEnabled,
            hasPasscode = true,
            hasRecoveryQuestion = true,
            recoveryQuestion = recoveryQuestion,
            isUnlocked = isUnlocked
        )
    }

    private fun matchesPasscode(passcode: String): Boolean {
        val storedHash = preferences.getString(KEY_PASSCODE_HASH, null).orEmpty()
        return storedHash.isNotBlank() && storedHash == hashPasscode(passcode.trim())
    }

    private fun matchesRecoveryAnswer(answer: String): Boolean {
        val storedHash = preferences.getString(KEY_RECOVERY_ANSWER_HASH, null).orEmpty()
        return storedHash.isNotBlank() &&
            storedHash == hashRecoveryAnswer(normalizeRecoveryAnswer(answer))
    }

    private fun hashPasscode(passcode: String): String {
        // Salt with device-specific + app-specific value to prevent rainbow table attacks
        val salt = preferences.getString(KEY_PASSCODE_SALT, null) ?: run {
            val newSalt = java.util.UUID.randomUUID().toString()
            preferences.edit().putString(KEY_PASSCODE_SALT, newSalt).apply()
            newSalt
        }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$passcode".toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun hashRecoveryAnswer(answer: String): String {
        val salt = preferences.getString(KEY_PASSCODE_SALT, null).orEmpty()
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:recovery:$answer".toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun normalizeRecoveryAnswer(answer: String): String {
        return answer.trim().lowercase()
    }

    private companion object {
        const val PREFERENCES_NAME = "radafiq_security"
        const val KEY_PASSCODE_HASH = "passcode_hash"
        const val KEY_PASSCODE_SALT = "passcode_salt"
        const val KEY_LOCK_ENABLED = "lock_enabled"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_RECOVERY_QUESTION = "recovery_question"
        const val KEY_RECOVERY_ANSWER_HASH = "recovery_answer_hash"
    }
}
