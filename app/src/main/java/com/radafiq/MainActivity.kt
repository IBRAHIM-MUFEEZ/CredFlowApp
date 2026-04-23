package com.radafiq

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.radafiq.data.backup.BackupJsonSerializer
import com.radafiq.data.backup.FileBackupRepository
import com.radafiq.data.models.CardSummary
import com.radafiq.data.profile.UserProfileRepository
import com.radafiq.data.repository.FirebaseRepository
import com.radafiq.data.security.AppSecurityRepository
import com.radafiq.data.settings.AppSettingsRepository
import com.radafiq.security.BiometricAuthManager
import com.radafiq.ui.AddPaymentScreen
import com.radafiq.ui.AddTransactionScreen
import com.radafiq.ui.AppLockScreen
import com.radafiq.ui.ChangePasscodeScreen
import com.radafiq.ui.RadafiqBackground
import com.radafiq.ui.RadafiqTheme
import com.radafiq.ui.DashboardScreen
import com.radafiq.ui.ProfileSetupScreen
import com.radafiq.ui.SecuritySetupScreen
import com.radafiq.ui.SettingsScreen
import com.radafiq.viewmodel.MainViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private var externalDocumentFlowInProgress = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            AppRoot()
        }
    }

    @Composable
    private fun AppRoot() {
        val settingsRepository = remember { AppSettingsRepository(applicationContext) }
        val securityRepository = remember { AppSecurityRepository(applicationContext) }
        val profileRepository = remember { UserProfileRepository() }
        val fileBackupRepository = remember { FileBackupRepository(applicationContext) }
        val firebaseRepository = remember { FirebaseRepository() }
        val mainViewModel: MainViewModel = viewModel()
        val biometricAuthManager = remember { BiometricAuthManager() }
        val navController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()

        val settingsState by settingsRepository.settings.collectAsState()
        val securityState by securityRepository.state.collectAsState()
        val profileState by profileRepository.state.collectAsState()
        val cards by mainViewModel.cards.collectAsState()

        var backupStatusMessage by rememberSaveable { mutableStateOf("") }
        var backupOperationInProgress by rememberSaveable { mutableStateOf(false) }
        var lockErrorMessage by rememberSaveable { mutableStateOf("") }
        val biometricAvailable = remember { biometricAuthManager.canAuthenticate(this@MainActivity) }
        val lockedAccountIds = remember(cards) {
            cards.filter(CardSummary::hasLedgerActivity).mapTo(linkedSetOf()) { it.id }
        }

        fun extractNestedMap(value: Any?): Map<String, Any?> {
            return (value as? Map<*, *>)?.entries
                ?.associate { entry -> entry.key.toString() to entry.value }
                .orEmpty()
        }

        fun defaultBackupFileName(): String {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            return "radafiq_backup_$timestamp.json"
        }

        suspend fun restoreBackupJson(
            backupJson: String,
            keepUnlocked: Boolean
        ) {
            withContext(Dispatchers.IO) {
                val payload = BackupJsonSerializer.fromJson(backupJson)
                firebaseRepository.restoreBackup(payload)
                val profileMap = payload.profile.filterValues { it != null }
                    .mapValues { it.value as Any }
                profileRepository.restoreProfileMap(profileMap)
                settingsRepository.restoreSettings(extractNestedMap(payload.settings["app"]))
                securityRepository.restoreSettings(extractNestedMap(payload.settings["security"]))
                if (keepUnlocked) {
                    securityRepository.unlock()
                }
            }
        }

        fun exportBackupToFile(uri: Uri) {
            coroutineScope.launch {
                backupOperationInProgress = true
                backupStatusMessage = "Exporting backup..."
                backupStatusMessage = runCatching {
                    withContext(Dispatchers.IO) {
                        val backupPayload = firebaseRepository.exportBackup(
                            profile = profileRepository.exportProfileMap(),
                            settings = mapOf(
                                "app" to settingsRepository.exportSettings(),
                                "security" to securityRepository.exportSettings()
                            )
                        )
                        val backupJson = BackupJsonSerializer.toJson(backupPayload)
                        fileBackupRepository.writeBackup(uri, backupJson).getOrThrow()
                    }
                    "Backup exported to file."
                }.getOrElse { throwable ->
                    "Backup export failed: ${throwable.localizedMessage ?: "unknown error"}"
                }
                backupOperationInProgress = false
            }
        }

        fun importBackupFromFile(uri: Uri) {
            coroutineScope.launch {
                val keepUnlocked = securityState.isUnlocked
                backupOperationInProgress = true
                backupStatusMessage = "Importing backup..."
                backupStatusMessage = runCatching {
                    val backupJson = fileBackupRepository.readBackup(uri).getOrThrow()
                    restoreBackupJson(
                        backupJson = backupJson,
                        keepUnlocked = keepUnlocked
                    )
                    "Backup restored from file."
                }.getOrElse { throwable ->
                    "Backup import failed: ${throwable.localizedMessage ?: "unknown error"}"
                }
                backupOperationInProgress = false
            }
        }

        val exportBackupLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            externalDocumentFlowInProgress = false
            if (uri != null) {
                exportBackupToFile(uri)
            } else {
                backupStatusMessage = "Backup export cancelled."
            }
        }

        val importBackupLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            externalDocumentFlowInProgress = false
            if (uri != null) {
                importBackupFromFile(uri)
            } else {
                backupStatusMessage = "Backup import cancelled."
            }
        }

        LaunchedEffect(Unit) {
            profileRepository.observeCurrentUserProfile()
        }

        DisposableEffect(
            securityState.lockEnabled,
            securityState.hasPasscode
        ) {
            val observer = LifecycleEventObserver { _, event ->
                if (
                    event == Lifecycle.Event.ON_STOP &&
                    securityState.lockEnabled &&
                    securityState.hasPasscode &&
                    !externalDocumentFlowInProgress
                ) {
                    securityRepository.lock()
                }
            }

            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

        val profile = profileState.profile
        val needsProfileSetup = profileState.isLoading.not() && profile?.isProfileComplete != true
        val needsSecuritySetup = profileState.isLoading.not() &&
            profile?.isProfileComplete == true &&
            !securityState.hasPasscode
        val requiresUnlock = profileState.isLoading.not() &&
            profile?.isProfileComplete == true &&
            securityState.lockEnabled &&
            securityState.hasPasscode &&
            !securityState.isUnlocked

        RadafiqTheme(themeMode = settingsState.themeMode) {
            when {
                profileState.isLoading -> {
                    RadafiqBackground {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                needsProfileSetup -> {
                    ProfileSetupScreen(
                        profile = profile,
                        onSave = { displayName, businessName, email ->
                            coroutineScope.launch {
                                profileRepository.saveProfile(displayName, businessName, email)
                            }
                        }
                    )
                }

                needsSecuritySetup -> {
                    SecuritySetupScreen(
                        biometricAvailable = biometricAvailable,
                        initialRecoveryQuestion = profile?.email?.takeIf { it.isNotBlank() }?.let {
                            "What is your email ID?"
                        }.orEmpty(),
                        onSave = { passcode, recoveryQuestion, recoveryAnswer, enableBiometric ->
                            securityRepository.setPasscode(
                                passcode = passcode,
                                recoveryQuestion = recoveryQuestion,
                                recoveryAnswer = recoveryAnswer
                            )
                            securityRepository.setBiometricEnabled(enableBiometric)
                            securityRepository.unlock()
                        }
                    )
                }

                requiresUnlock -> {
                    AppLockScreen(
                        biometricAvailable = biometricAvailable,
                        biometricEnabled = securityState.biometricEnabled && biometricAvailable,
                        recoveryQuestion = securityState.recoveryQuestion,
                        errorMessage = lockErrorMessage,
                        onUnlockWithPasscode = { passcode ->
                            val unlocked = securityRepository.verifyPasscode(passcode)
                            lockErrorMessage = if (unlocked) "" else "Incorrect passcode."
                            unlocked
                        },
                        onUnlockWithBiometric = if (securityState.biometricEnabled && biometricAvailable) {
                            {
                                biometricAuthManager.authenticate(
                                    activity = this@MainActivity,
                                    title = "Unlock Radafiq",
                                    subtitle = "Verify with fingerprint, face unlock, or device credential.",
                                    onSuccess = {
                                        lockErrorMessage = ""
                                        securityRepository.unlock()
                                    },
                                    onFailure = { message ->
                                        lockErrorMessage = message
                                    }
                                )
                            }
                        } else {
                            null
                        },
                        onResetWithRecovery = if (securityState.hasRecoveryQuestion) {
                            { recoveryAnswer, newPasscode, enableBiometric ->
                                val reset = securityRepository.resetPasscodeWithRecovery(
                                    recoveryAnswer = recoveryAnswer,
                                    newPasscode = newPasscode
                                )
                                if (reset) {
                                    securityRepository.setBiometricEnabled(enableBiometric)
                                    securityRepository.unlock()
                                    lockErrorMessage = ""
                                } else {
                                    lockErrorMessage = "Recovery answer is incorrect."
                                }
                                reset
                            }
                        } else {
                            null
                        }
                    )
                }

                else -> {
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                navController = navController,
                                selectedAccountIds = settingsState.selectedAccountIds,
                                onOpenSettings = { navController.navigate("settings") },
                                profileName = profile?.displayName.orEmpty(),
                                vm = mainViewModel
                            )
                        }

                        composable("addTransaction") {
                            AddTransactionScreen {
                                navController.popBackStack()
                            }
                        }

                        composable("addPayment") {
                            AddPaymentScreen(
                                selectedAccountIds = settingsState.selectedAccountIds
                            ) {
                                navController.popBackStack()
                            }
                        }

                        composable("profile") {
                            ProfileSetupScreen(
                                profile = profile,
                                onSave = { displayName, businessName, email ->
                                    coroutineScope.launch {
                                        profileRepository.saveProfile(displayName, businessName, email)
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable("securitySetup") {
                            if (securityState.hasPasscode) {
                                ChangePasscodeScreen(
                                    biometricAvailable = biometricAvailable,
                                    biometricEnabled = securityState.biometricEnabled,
                                    currentRecoveryQuestion = securityState.recoveryQuestion,
                                    onSave = { currentPasscode, newPasscode, recoveryQuestion, recoveryAnswer, enableBiometric ->
                                        val updated = securityRepository.updatePasscode(
                                            currentPasscode = currentPasscode,
                                            newPasscode = newPasscode,
                                            recoveryQuestion = recoveryQuestion,
                                            recoveryAnswer = recoveryAnswer
                                        )
                                        if (updated) {
                                            securityRepository.setBiometricEnabled(enableBiometric)
                                            securityRepository.unlock()
                                            navController.popBackStack()
                                        }
                                        updated
                                    }
                                )
                            } else {
                                SecuritySetupScreen(
                                    biometricAvailable = biometricAvailable,
                                    initialRecoveryQuestion = profile?.email?.takeIf { it.isNotBlank() }?.let {
                                        "What is your email ID?"
                                    }.orEmpty(),
                                    onSave = { passcode, recoveryQuestion, recoveryAnswer, enableBiometric ->
                                        securityRepository.setPasscode(
                                            passcode = passcode,
                                            recoveryQuestion = recoveryQuestion,
                                            recoveryAnswer = recoveryAnswer
                                        )
                                        securityRepository.setBiometricEnabled(enableBiometric)
                                        securityRepository.unlock()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        composable("settings") {
                            SettingsScreen(
                                settingsState = settingsState,
                                profile = profile,
                                securityState = securityState,
                                lockedAccountIds = lockedAccountIds,
                                backupStatusMessage = backupStatusMessage,
                                isBackupOperationInProgress = backupOperationInProgress,
                                onThemeModeSelected = settingsRepository::setThemeMode,
                                onAccountSelectionChanged = settingsRepository::setAccountSelected,
                                onLockEnabledChanged = securityRepository::setLockEnabled,
                                onBiometricEnabledChanged = securityRepository::setBiometricEnabled,
                                onEditProfile = { navController.navigate("profile") },
                                onOpenSecuritySetup = { navController.navigate("securitySetup") },
                                onBackupToDrive = {
                                    backupStatusMessage = "Choose where to save your backup."
                                    externalDocumentFlowInProgress = true
                                    runCatching {
                                        exportBackupLauncher.launch(defaultBackupFileName())
                                    }.onFailure { throwable ->
                                        externalDocumentFlowInProgress = false
                                        backupStatusMessage = "Unable to open export dialog: ${throwable.localizedMessage ?: "unknown error"}"
                                    }
                                },
                                onRestoreFromDrive = {
                                    backupStatusMessage = "Select a backup file to import."
                                    externalDocumentFlowInProgress = true
                                    runCatching {
                                        importBackupLauncher.launch(
                                            arrayOf(
                                                "application/json",
                                                "application/octet-stream",
                                                "text/plain",
                                                "*/*"
                                            )
                                        )
                                    }.onFailure { throwable ->
                                        externalDocumentFlowInProgress = false
                                        backupStatusMessage = "Unable to open import dialog: ${throwable.localizedMessage ?: "unknown error"}"
                                    }
                                }
                            ) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun CardSummary.hasLedgerActivity(): Boolean {
    return bill > 0.0 ||
        pending > 0.0 ||
        dueAmount > 0.0 ||
        dueDate.isNotBlank() ||
        remindersEnabled ||
        reminderEmail.isNotBlank() ||
        reminderWhatsApp.isNotBlank()
}
