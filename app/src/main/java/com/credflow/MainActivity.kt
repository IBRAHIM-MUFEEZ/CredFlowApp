package com.credflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.credflow.ui.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CredFlowTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "dashboard") {

                    composable("dashboard") {
                        DashboardScreen(navController)
                    }

                    composable("addTransaction") {
                        AddTransactionScreen {
                            navController.popBackStack()
                        }
                    }

                    composable("addPayment") {
                        AddPaymentScreen {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}
