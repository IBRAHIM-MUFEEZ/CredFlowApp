package com.credflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import com.credflow.data.settings.AppSettingsRepository
import com.credflow.ui.AddPaymentScreen
import com.credflow.ui.AddTransactionScreen
import com.credflow.ui.CredFlowTheme
import com.credflow.ui.DashboardScreen
import com.credflow.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            val settingsRepository = remember { AppSettingsRepository(applicationContext) }
            val settingsState by settingsRepository.settings.collectAsState()

            CredFlowTheme(themeMode = settingsState.themeMode) {
                val navController = rememberNavController()

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

                    composable("settings") {
                        SettingsScreen(
                            settingsState = settingsState,
                            onThemeModeSelected = settingsRepository::setThemeMode,
                            onAccountSelectionChanged = settingsRepository::setAccountSelected
                        ) {
                            navController.popBackStack()
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
