package com.radafiq.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
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
    private val appContext = context.applicationContext
    private val legacyPreferences: SharedPreferences = appContext.getSharedPreferences(
        LEGACY_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val preferences: SharedPreferences = createSecurePreferences(appContext)

    private val _state: MutableStateFlow<AppSecurityState>
    val state: StateFlow<AppSecurityState>

    init {
        migrateLegacyPreferencesIfNeeded()
        _state = MutableStateFlow(loadState())
        state = _state.asStateFlow()
    }

    fun setPasscode(
        passcode: String,
        recoveryQuestion: String,
        recoveryAnswer: String
    ) {
        val normalized = passcode.trim()
        val normalizedQuestion = recoveryQuestion.trim()
        val normalizedAnswer = normalizeRecoveryAnswer(recoveryAnswer)
        if (
            !isValidPasscode(normalized) ||
            normalizedQuestion.isBlank() ||
            normalizedAnswer.length < MIN_RECOVERY_ANSWER_LENGTH
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
            !isValidPasscode(normalized) ||
            normalizedQuestion.isBlank() ||
            normalizedAnswer.length < MIN_RECOVERY_ANSWER_LENGTH
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
            .remove(KEY_PASSCODE_SALT)
            .remove(KEY_RECOVERY_ANSWER_SALT)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL_MS)
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
        // FIX-2: Brute-force rate limiting — track failed attempts with exponential back-off.
        val attempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0)
        val lockedUntil = preferences.getLong(KEY_LOCKED_UNTIL_MS, 0L)
        val now = System.currentTimeMillis()

        if (now < lockedUntil) {
            // Still locked out — reject immediately without checking the passcode
            return false
        }

        val matches = matchesPasscode(passcode)
        if (matches) {
            // Reset failure counter on success
            preferences.edit()
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKED_UNTIL_MS)
                .apply()
            unlock()
        } else {
            val newAttempts = attempts + 1
            // Lockout durations: 5 fails→30s, 7 fails→2min, 10+ fails→10min
            val lockDurationMs = when {
                newAttempts >= 10 -> 10 * 60 * 1000L
                newAttempts >= 7  ->  2 * 60 * 1000L
                newAttempts >= 5  ->      30 * 1000L
                else              -> 0L
            }
            preferences.edit()
                .putInt(KEY_FAILED_ATTEMPTS, newAttempts)
                .putLong(KEY_LOCKED_UNTIL_MS, if (lockDurationMs > 0) now + lockDurationMs else 0L)
                .apply()
        }
        return matches
    }

    /**
     * Returns how many milliseconds remain in the current lockout period, or 0 if not locked.
     * UI can call this to show a countdown to the user.
     */
    fun lockoutRemainingMs(): Long {
        val lockedUntil = preferences.getLong(KEY_LOCKED_UNTIL_MS, 0L)
        return (lockedUntil - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /** Returns the current failed-attempt count (for UI display). */
    fun failedAttemptCount(): Int = preferences.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun resetPasscodeWithRecovery(
        recoveryAnswer: String,
        newPasscode: String
    ): Boolean {
        val currentRecoveryQuestion = _state.value.recoveryQuestion
        if (
            !_state.value.hasRecoveryQuestion ||
            !matchesRecoveryAnswer(recoveryAnswer) ||
            !isValidPasscode(newPasscode.trim()) ||
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
        // Passcode and recovery hashes are intentionally not exported. A 6 digit
        // lock code is easy to brute force offline if a backup file is leaked.
        return mapOf(
            KEY_LOCK_ENABLED to false,
            KEY_BIOMETRIC_ENABLED to false
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
            if (hasPasscode) putString(KEY_PASSCODE_HASH, passcodeHash) else remove(KEY_PASSCODE_HASH)
            if (passcodeSalt.isNotBlank()) putString(KEY_PASSCODE_SALT, passcodeSalt) else remove(KEY_PASSCODE_SALT)
            if (recoveryQuestion.isNotBlank()) putString(KEY_RECOVERY_QUESTION, recoveryQuestion) else remove(KEY_RECOVERY_QUESTION)
            if (recoveryAnswerHash.isNotBlank()) putString(KEY_RECOVERY_ANSWER_HASH, recoveryAnswerHash) else remove(KEY_RECOVERY_ANSWER_HASH)
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
        if (storedHash.isBlank()) return false

        val normalized = passcode.trim()
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            return constantTimeEquals(storedHash, hashPasscode(normalized))
        }

        val matchesLegacy = constantTimeEquals(storedHash, legacyHashPasscode(normalized))
        if (matchesLegacy) {
            preferences.edit().putString(KEY_PASSCODE_HASH, hashPasscode(normalized)).apply()
        }
        return matchesLegacy
    }

    private fun matchesRecoveryAnswer(answer: String): Boolean {
        val storedHash = preferences.getString(KEY_RECOVERY_ANSWER_HASH, null).orEmpty()
        if (storedHash.isBlank()) return false

        val normalized = normalizeRecoveryAnswer(answer)
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            return constantTimeEquals(storedHash, hashRecoveryAnswer(normalized))
        }

        val matchesLegacy = constantTimeEquals(storedHash, legacyHashRecoveryAnswer(normalized))
        if (matchesLegacy) {
            preferences.edit().putString(KEY_RECOVERY_ANSWER_HASH, hashRecoveryAnswer(normalized)).apply()
        }
        return matchesLegacy
    }

    private fun hashPasscode(passcode: String): String {
        return pbkdf2(passcode, getOrCreateSalt())
    }

    private fun hashRecoveryAnswer(answer: String): String {
        // FIX-3: Use a separate salt for the recovery answer so it cannot be attacked
        // together with the passcode even if the passcode salt is known.
        return pbkdf2("recovery:$answer", getOrCreateRecoverySalt())
    }

    private fun legacyHashPasscode(passcode: String): String {
        val salt = preferences.getString(KEY_PASSCODE_SALT, null).orEmpty()
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$passcode".toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun legacyHashRecoveryAnswer(answer: String): String {
        // Legacy: used the passcode salt — kept only for migration
        val salt = preferences.getString(KEY_PASSCODE_SALT, null).orEmpty()
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:recovery:$answer".toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun getOrCreateSalt(): String {
        return preferences.getString(KEY_PASSCODE_SALT, null) ?: run {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            val newSalt = bytes.toHex()
            preferences.edit().putString(KEY_PASSCODE_SALT, newSalt).apply()
            newSalt
        }
    }

    // FIX-3: Separate salt for recovery answer
    private fun getOrCreateRecoverySalt(): String {
        return preferences.getString(KEY_RECOVERY_ANSWER_SALT, null) ?: run {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            val newSalt = bytes.toHex()
            preferences.edit().putString(KEY_RECOVERY_ANSWER_SALT, newSalt).apply()
            newSalt
        }
    }

    private fun pbkdf2(value: String, salt: String): String {
        val spec = PBEKeySpec(
            value.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH_BITS
        )
        val bytes = SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
        return "$PBKDF2_PREFIX$PBKDF2_ITERATIONS:${bytes.toHex()}"
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(
            left.toByteArray(Charsets.UTF_8),
            right.toByteArray(Charsets.UTF_8)
        )
    }

    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun isValidPasscode(passcode: String): Boolean {
        return passcode.length == PASSCODE_LENGTH && passcode.all(Char::isDigit)
    }

    private fun normalizeRecoveryAnswer(answer: String): String {
        return answer.trim().lowercase()
    }

    private fun createSecurePreferences(context: Context): SharedPreferences {
        return runCatching {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                ENCRYPTED_PREFERENCES_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse { e ->
            // FIX-1: Log the failure so it is visible in crash reports.
            // We do NOT silently fall back to plain SharedPreferences — instead we
            // attempt a keystore key rotation (delete + recreate) and retry once.
            android.util.Log.e("AppSecurity", "EncryptedSharedPreferences creation failed: ${e.localizedMessage}", e)
            runCatching {
                // Attempt key rotation: delete the old master key and recreate
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                if (keyStore.containsAlias(MasterKeys.AES256_GCM_SPEC.keystoreAlias)) {
                    keyStore.deleteEntry(MasterKeys.AES256_GCM_SPEC.keystoreAlias)
                }
                // Also delete the corrupted prefs file so it can be recreated cleanly
                context.deleteSharedPreferences(ENCRYPTED_PREFERENCES_NAME)
                val newMasterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    ENCRYPTED_PREFERENCES_NAME,
                    newMasterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.getOrElse { e2 ->
                // Key rotation also failed — this device's keystore is broken.
                // Security is degraded; log prominently and use plain prefs as last resort.
                android.util.Log.e("AppSecurity", "Key rotation failed — security degraded: ${e2.localizedMessage}", e2)
                context.getSharedPreferences(ENCRYPTED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun migrateLegacyPreferencesIfNeeded() {
        if (preferences.contains(KEY_PASSCODE_HASH) || !legacyPreferences.contains(KEY_PASSCODE_HASH)) {
            return
        }

        preferences.edit().apply {
            copyString(KEY_PASSCODE_HASH)
            copyString(KEY_PASSCODE_SALT)
            copyString(KEY_RECOVERY_QUESTION)
            copyString(KEY_RECOVERY_ANSWER_HASH)
            putBoolean(KEY_LOCK_ENABLED, legacyPreferences.getBoolean(KEY_LOCK_ENABLED, false))
            putBoolean(KEY_BIOMETRIC_ENABLED, legacyPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false))
            apply()
        }

        legacyPreferences.edit().clear().apply()
    }

    private fun SharedPreferences.Editor.copyString(key: String) {
        legacyPreferences.getString(key, null)?.let { putString(key, it) }
    }

    private companion object {
        const val LEGACY_PREFERENCES_NAME = "radafiq_security"
        const val ENCRYPTED_PREFERENCES_NAME = "radafiq_security_secure"
        const val KEY_PASSCODE_HASH = "passcode_hash"
        const val KEY_PASSCODE_SALT = "passcode_salt"
        const val KEY_RECOVERY_ANSWER_SALT = "recovery_answer_salt"   // FIX-3: separate salt
        const val KEY_LOCK_ENABLED = "lock_enabled"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_RECOVERY_QUESTION = "recovery_question"
        const val KEY_RECOVERY_ANSWER_HASH = "recovery_answer_hash"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"              // FIX-2: brute-force counter
        const val KEY_LOCKED_UNTIL_MS = "locked_until_ms"             // FIX-2: lockout timestamp
        const val PASSCODE_LENGTH = 6
        const val MIN_RECOVERY_ANSWER_LENGTH = 3
        const val PBKDF2_ITERATIONS = 150_000
        const val PBKDF2_KEY_LENGTH_BITS = 256
        const val PBKDF2_PREFIX = "pbkdf2:"
    }
}
