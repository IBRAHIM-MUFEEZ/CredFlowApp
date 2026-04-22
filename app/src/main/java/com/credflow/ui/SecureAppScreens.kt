package com.credflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.credflow.data.profile.UserProfile

@Composable
fun SessionBootstrapScreen(
    isLoading: Boolean,
    errorMessage: String,
    onRetry: () -> Unit
) {
    CredFlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            PageHeader(
                title = "Preparing CredFlow",
                subtitle = "CredFlow now opens without mobile number sign-in and creates your secure app session automatically."
            )

            HeroPanel(
                title = "Automatic startup",
                amount = if (isLoading) "SYNC" else "RETRY",
                subtitle = "Your profile, ledger data, and backups stay connected without any sign-in prompt."
            )

            FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
                Text(
                    text = "Starting session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isLoading) {
                        "Setting up your secure workspace."
                    } else {
                        errorMessage.ifBlank { "CredFlow couldn't start the app session." }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLoading) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    profile: UserProfile?,
    onSave: (displayName: String, businessName: String, email: String) -> Unit
) {
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
    var businessName by remember(profile?.businessName) { mutableStateOf(profile?.businessName.orEmpty()) }
    var email by remember(profile?.email) { mutableStateOf(profile?.email.orEmpty()) }

    CredFlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            PageHeader(
                title = "Set Up Profile",
                subtitle = "Create the business identity that appears across your ledgers, reminders, and backups."
            )

            FlowCard(accentColor = MaterialTheme.colorScheme.secondary) {
                Text(
                    text = "Profile details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business / Shop Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSave(displayName, businessName, email) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = displayName.isNotBlank() && businessName.isNotBlank()
                ) {
                    Text("Save Profile")
                }
            }
        }
    }
}

@Composable
fun SecuritySetupScreen(
    biometricAvailable: Boolean,
    onSave: (passcode: String, enableBiometric: Boolean) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var useBiometric by remember(biometricAvailable) { mutableStateOf(biometricAvailable) }

    val passcodesMatch = passcode.length >= 4 && passcode == confirmPasscode

    CredFlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            PageHeader(
                title = "Protect the App",
                subtitle = "Add a passcode and optionally enable fingerprint or face unlock for every launch."
            )

            FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it.take(6) },
                    label = { Text("Create Passcode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { confirmPasscode = it.take(6) },
                    label = { Text("Confirm Passcode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use fingerprint / face unlock",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (biometricAvailable) {
                                "Biometric unlock is available on this device."
                            } else {
                                "Biometric unlock is not available on this device."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useBiometric && biometricAvailable,
                        onCheckedChange = { useBiometric = it },
                        enabled = biometricAvailable
                    )
                }

                if (passcode.isNotBlank() && !passcodesMatch) {
                    Text(
                        text = "Passcodes must match and contain at least 4 digits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSave(passcode, useBiometric && biometricAvailable) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = passcodesMatch
                ) {
                    Text("Save Security Setup")
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    errorMessage: String,
    onUnlockWithPasscode: (String) -> Boolean,
    onUnlockWithBiometric: (() -> Unit)?
) {
    var passcode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    CredFlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CredFlow Locked",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Enter your passcode or verify with biometrics to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            FlowCard(
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it.take(6)
                        localError = ""
                    },
                    label = { Text("Passcode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val unlocked = onUnlockWithPasscode(passcode)
                        if (!unlocked) {
                            localError = "Incorrect passcode."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock")
                }

                if (biometricEnabled && onUnlockWithBiometric != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onUnlockWithBiometric,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Fingerprint / Face Unlock")
                    }
                }

                val message = localError.ifBlank { errorMessage }
                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
