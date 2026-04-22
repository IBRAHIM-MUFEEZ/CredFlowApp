package com.credflow

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
import com.credflow.data.backup.BackupJsonSerializer
import com.credflow.data.backup.FileBackupRepository
import com.credflow.data.models.CardSummary
import com.credflow.data.profile.UserProfileRepository
import com.credflow.data.repository.FirebaseRepository
import com.credflow.data.security.AppSecurityRepository
import com.credflow.data.settings.AppSettingsRepository
import com.credflow.security.BiometricAuthManager
import com.credflow.ui.AddPaymentScreen
import com.credflow.ui.AddTransactionScreen
import com.credflow.ui.AppLockScreen
import com.credflow.ui.CredFlowBackground
import com.credflow.ui.CredFlowTheme
import com.credflow.ui.DashboardScreen
import com.credflow.ui.ProfileSetupScreen
import com.credflow.ui.SecuritySetupScreen
import com.credflow.ui.SettingsScreen
import com.credflow.viewmodel.MainViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

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
            return "dafira_backup_$timestamp.json"
        }

        suspend fun restoreBackupJson(backupJson: String) {
            val payload = BackupJsonSerializer.fromJson(backupJson)
            firebaseRepository.restoreBackup(payload)
            val profileMap = payload.profile.filterValues { it != null }
                .mapValues { it.value as Any }
            profileRepository.restoreProfileMap(profileMap)
            settingsRepository.restoreSettings(extractNestedMap(payload.settings["app"]))
            securityRepository.restoreSettings(extractNestedMap(payload.settings["security"]))
        }

        fun exportBackupToFile(uri: Uri) {
            coroutineScope.launch {
                backupStatusMessage = runCatching {
                    val backupPayload = firebaseRepository.exportBackup(
                        profile = profileRepository.exportProfileMap(),
                        settings = mapOf(
                            "app" to settingsRepository.exportSettings(),
                            "security" to securityRepository.exportSettings()
                        )
                    )
                    val backupJson = BackupJsonSerializer.toJson(backupPayload)
                    fileBackupRepository.writeBackup(uri, backupJson).getOrThrow()
                    "Backup exported to file."
                }.getOrElse { throwable ->
                    "Backup export failed: ${throwable.localizedMessage ?: "unknown error"}"
                }
            }
        }

        fun importBackupFromFile(uri: Uri) {
            coroutineScope.launch {
                backupStatusMessage = runCatching {
                    val backupJson = fileBackupRepository.readBackup(uri).getOrThrow()
                    restoreBackupJson(backupJson)
                    "Backup restored from file."
                }.getOrElse { throwable ->
                    "Backup import failed: ${throwable.localizedMessage ?: "unknown error"}"
                }
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

        CredFlowTheme(themeMode = settingsState.themeMode) {
            when {
                profileState.isLoading -> {
                    CredFlowBackground {
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
                        onSave = { passcode, enableBiometric ->
                            securityRepository.setPasscode(passcode)
                            securityRepository.setBiometricEnabled(enableBiometric)
                            securityRepository.unlock()
                        }
                    )
                }

                requiresUnlock -> {
                    AppLockScreen(
                        biometricEnabled = securityState.biometricEnabled && biometricAvailable,
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
                                    title = "Unlock Dafira",
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
                            SecuritySetupScreen(
                                biometricAvailable = biometricAvailable,
                                onSave = { passcode, enableBiometric ->
                                    securityRepository.setPasscode(passcode)
                                    securityRepository.setBiometricEnabled(enableBiometric)
                                    securityRepository.unlock()
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settingsState = settingsState,
                                profile = profile,
                                securityState = securityState,
                                lockedAccountIds = lockedAccountIds,
                                backupStatusMessage = backupStatusMessage,
                                onThemeModeSelected = settingsRepository::setThemeMode,
                                onAccountSelectionChanged = settingsRepository::setAccountSelected,
                                onLockEnabledChanged = securityRepository::setLockEnabled,
                                onBiometricEnabledChanged = securityRepository::setBiometricEnabled,
                                onEditProfile = { navController.navigate("profile") },
                                onOpenSecuritySetup = { navController.navigate("securitySetup") },
                                onBackupToDrive = {
                                    backupStatusMessage = "Preparing backup export..."
                                    externalDocumentFlowInProgress = true
                                    runCatching {
                                        exportBackupLauncher.launch(defaultBackupFileName())
                                    }.onFailure { throwable ->
                                        externalDocumentFlowInProgress = false
                                        backupStatusMessage = "Unable to open export dialog: ${throwable.localizedMessage ?: "unknown error"}"
                                    }
                                },
                                onRestoreFromDrive = {
                                    backupStatusMessage = "Preparing backup import..."
                                    externalDocumentFlowInProgress = true
                                    runCatching {
                                        importBackupLauncher.launch(
                                            arrayOf(
                                                "application/json",
                                                "application/octet-stream",
                                                "text/plain"
                                            )
                                        )
                                    }.onFailure { throwable ->
                                        externalDocumentFlowInProgress = false
                                        backupStatusMessage = "Unable to open import dialog: ${throwable.localizedMessage ?: "unknown error"}"
                                    }
                                },
                                onClearPasscode = { securityRepository.clearPasscode() }
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
