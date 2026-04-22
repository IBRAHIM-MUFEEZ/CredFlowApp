package com.credflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.credflow.data.auth.SessionRepository
import com.credflow.data.backup.BackupJsonSerializer
import com.credflow.data.backup.DriveBackupRepository
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
import com.credflow.ui.SessionBootstrapScreen
import com.credflow.ui.SettingsScreen
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

class MainActivity : FragmentActivity() {
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
        val sessionRepository = remember { SessionRepository() }
        val profileRepository = remember { UserProfileRepository() }
        val driveBackupRepository = remember { DriveBackupRepository() }
        val firebaseRepository = remember { FirebaseRepository() }
        val biometricAuthManager = remember { BiometricAuthManager() }
        val navController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()

        val settingsState by settingsRepository.settings.collectAsState()
        val securityState by securityRepository.state.collectAsState()
        val sessionState by sessionRepository.state.collectAsState()
        val profileState by profileRepository.state.collectAsState()

        var backupStatusMessage by rememberSaveable { mutableStateOf("") }
        var lockErrorMessage by rememberSaveable { mutableStateOf("") }
        var pendingDriveAction by remember { mutableStateOf<DriveAction?>(null) }

        val authorizationClient = remember { Identity.getAuthorizationClient(this@MainActivity) }
        val biometricAvailable = remember { biometricAuthManager.canAuthenticate(this@MainActivity) }

        fun extractNestedMap(value: Any?): Map<String, Any?> {
            return (value as? Map<*, *>)?.entries
                ?.associate { entry -> entry.key.toString() to entry.value }
                .orEmpty()
        }

        fun runDriveAction(accessToken: String, action: DriveAction) {
            coroutineScope.launch {
                when (action) {
                    DriveAction.BACKUP -> {
                        val backupPayload = firebaseRepository.exportBackup(
                            profile = profileRepository.exportProfileMap(),
                            settings = mapOf(
                                "app" to settingsRepository.exportSettings(),
                                "security" to securityRepository.exportSettings()
                            )
                        )
                        val backupJson = BackupJsonSerializer.toJson(backupPayload)
                        val result = driveBackupRepository.uploadBackup(accessToken, backupJson)
                        backupStatusMessage = result.fold(
                            onSuccess = { "Backup uploaded to Google Drive app storage." },
                            onFailure = { throwable ->
                                "Backup failed: ${throwable.localizedMessage ?: "unknown error"}"
                            }
                        )
                    }

                    DriveAction.RESTORE -> {
                        val result = driveBackupRepository.downloadLatestBackup(accessToken)
                        result.fold(
                            onSuccess = { backupJson ->
                                val payload = BackupJsonSerializer.fromJson(backupJson)
                                firebaseRepository.restoreBackup(payload)
                                val profileMap = payload.profile.filterValues { it != null }
                                    .mapValues { it.value as Any }
                                profileRepository.restoreProfileMap(profileMap)
                                settingsRepository.restoreSettings(extractNestedMap(payload.settings["app"]))
                                securityRepository.restoreSettings(extractNestedMap(payload.settings["security"]))
                                backupStatusMessage = "Backup restored from Google Drive."
                            },
                            onFailure = { throwable ->
                                backupStatusMessage = "Restore failed: ${throwable.localizedMessage ?: "unknown error"}"
                            }
                        )
                    }
                }
            }
        }

        val driveAuthorizationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            try {
                val authorizationResult = authorizationClient.getAuthorizationResultFromIntent(activityResult.data)
                val accessToken = authorizationResult.accessToken
                val action = pendingDriveAction
                if (!accessToken.isNullOrBlank() && action != null) {
                    runDriveAction(accessToken, action)
                } else {
                    backupStatusMessage = "Authorization completed, but no access token was returned."
                }
            } catch (exception: ApiException) {
                backupStatusMessage = exception.localizedMessage ?: "Google authorization failed."
            } finally {
                pendingDriveAction = null
            }
        }

        fun requestDriveAuthorization(action: DriveAction) {
            pendingDriveAction = action
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()

            authorizationClient.authorize(request)
                .addOnSuccessListener { authorizationResult ->
                    when {
                        authorizationResult.hasResolution() -> {
                            val pendingIntent = authorizationResult.pendingIntent
                            if (pendingIntent != null) {
                                driveAuthorizationLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            } else {
                                backupStatusMessage = "Authorization requires user approval, but the request could not be opened."
                                pendingDriveAction = null
                            }
                        }

                        !authorizationResult.accessToken.isNullOrBlank() -> {
                            runDriveAction(authorizationResult.accessToken!!, action)
                            pendingDriveAction = null
                        }

                        else -> {
                            backupStatusMessage = "No Google Drive access token was returned."
                            pendingDriveAction = null
                        }
                    }
                }
                .addOnFailureListener { throwable ->
                    backupStatusMessage = throwable.localizedMessage ?: "Unable to authorize Google Drive."
                    pendingDriveAction = null
                }
        }

        LaunchedEffect(sessionState.user?.uid) {
            profileRepository.observeCurrentUserProfile()
            if (sessionState.user == null) {
                backupStatusMessage = ""
                lockErrorMessage = ""
            }
        }

        DisposableEffect(sessionState.user?.uid, securityState.lockEnabled, securityState.hasPasscode) {
            val observer = LifecycleEventObserver { _, event ->
                if (
                    event == Lifecycle.Event.ON_STOP &&
                    sessionState.user != null &&
                    securityState.lockEnabled &&
                    securityState.hasPasscode
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
        val needsProfileSetup = sessionState.user != null && profileState.isLoading.not() && profile?.isProfileComplete != true
        val needsSecuritySetup = sessionState.user != null &&
            profile?.isProfileComplete == true &&
            !securityState.hasPasscode
        val requiresUnlock = sessionState.user != null &&
            profile?.isProfileComplete == true &&
            securityState.lockEnabled &&
            securityState.hasPasscode &&
            !securityState.isUnlocked

        CredFlowTheme(themeMode = settingsState.themeMode) {
            when {
                sessionState.user == null -> {
                    SessionBootstrapScreen(
                        isLoading = sessionState.isLoading,
                        errorMessage = sessionState.errorMessage,
                        onRetry = sessionRepository::ensureSession
                    )
                }

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
                                    title = "Unlock CredFlow",
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
                                onOpenSettings = { navController.navigate("settings") }
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
                                backupStatusMessage = backupStatusMessage,
                                onThemeModeSelected = settingsRepository::setThemeMode,
                                onAccountSelectionChanged = settingsRepository::setAccountSelected,
                                onLockEnabledChanged = securityRepository::setLockEnabled,
                                onBiometricEnabledChanged = securityRepository::setBiometricEnabled,
                                onEditProfile = { navController.navigate("profile") },
                                onOpenSecuritySetup = { navController.navigate("securitySetup") },
                                onBackupToDrive = { requestDriveAuthorization(DriveAction.BACKUP) },
                                onRestoreFromDrive = { requestDriveAuthorization(DriveAction.RESTORE) },
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

private enum class DriveAction {
    BACKUP,
    RESTORE
}
