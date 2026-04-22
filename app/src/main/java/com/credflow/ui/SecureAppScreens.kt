package com.credflow.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.credflow.R
import com.credflow.data.profile.UserProfile

private val RecoveryQuestions = listOf(
    "What is your email ID?",
    "What was your first pet's name?",
    "What city were you born in?",
    "What is your mother's first name?"
)

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
    initialRecoveryQuestion: String = RecoveryQuestions.first(),
    onSave: (
        passcode: String,
        recoveryQuestion: String,
        recoveryAnswer: String,
        enableBiometric: Boolean
    ) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    val availableQuestions = remember(initialRecoveryQuestion) {
        (listOf(initialRecoveryQuestion).filter { it.isNotBlank() } + RecoveryQuestions).distinct()
    }
    var selectedRecoveryQuestion by remember(initialRecoveryQuestion) {
        mutableStateOf(availableQuestions.firstOrNull().orEmpty())
    }
    var recoveryAnswer by remember { mutableStateOf("") }
    var useBiometric by remember(biometricAvailable) { mutableStateOf(biometricAvailable) }

    val passcodesMatch = passcode.length >= 4 && passcode == confirmPasscode
    val hasRecoveryDetails = selectedRecoveryQuestion.isNotBlank() && recoveryAnswer.isNotBlank()

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
                subtitle = "Add a passcode, choose a mandatory recovery question, and optionally enable fingerprint or face unlock for every launch."
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

                Spacer(modifier = Modifier.height(12.dp))

                RecoveryQuestionDropdown(
                    questions = availableQuestions,
                    selectedQuestion = selectedRecoveryQuestion,
                    onQuestionSelected = { selectedRecoveryQuestion = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryAnswer,
                    onValueChange = { recoveryAnswer = it },
                    label = { Text("Recovery Answer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Forgot passcode recovery works only through this answer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
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
                    onClick = {
                        onSave(
                            passcode,
                            selectedRecoveryQuestion,
                            recoveryAnswer,
                            useBiometric && biometricAvailable
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = passcodesMatch && hasRecoveryDetails
                ) {
                    Text("Save Security Setup")
                }
            }
        }
    }
}

@Composable
fun ChangePasscodeScreen(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    currentRecoveryQuestion: String,
    onSave: (
        currentPasscode: String,
        newPasscode: String,
        recoveryQuestion: String,
        recoveryAnswer: String,
        enableBiometric: Boolean
    ) -> Boolean
) {
    var currentPasscode by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    val availableQuestions = remember(currentRecoveryQuestion) {
        (listOf(currentRecoveryQuestion).filter { it.isNotBlank() } + RecoveryQuestions).distinct()
    }
    var selectedRecoveryQuestion by remember(currentRecoveryQuestion) {
        mutableStateOf(availableQuestions.firstOrNull().orEmpty())
    }
    var recoveryAnswer by remember { mutableStateOf("") }
    var useBiometric by remember(biometricAvailable, biometricEnabled) {
        mutableStateOf(biometricEnabled && biometricAvailable)
    }
    var localError by remember { mutableStateOf("") }

    val passcodesMatch = newPasscode.length >= 4 && newPasscode == confirmPasscode
    val canSave = currentPasscode.isNotBlank() &&
        passcodesMatch &&
        selectedRecoveryQuestion.isNotBlank() &&
        recoveryAnswer.isNotBlank()

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
                title = "Change Passcode",
                subtitle = "Enter the existing passcode first, then save a new passcode and mandatory recovery question details."
            )

            FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
                OutlinedTextField(
                    value = currentPasscode,
                    onValueChange = {
                        currentPasscode = it.take(6)
                        localError = ""
                    },
                    label = { Text("Existing Passcode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPasscode,
                    onValueChange = {
                        newPasscode = it.take(6)
                        localError = ""
                    },
                    label = { Text("New Passcode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = {
                        confirmPasscode = it.take(6)
                        localError = ""
                    },
                    label = { Text("Confirm New Passcode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                RecoveryQuestionDropdown(
                    questions = availableQuestions,
                    selectedQuestion = selectedRecoveryQuestion,
                    onQuestionSelected = {
                        selectedRecoveryQuestion = it
                        localError = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryAnswer,
                    onValueChange = {
                        recoveryAnswer = it
                        localError = ""
                    },
                    label = { Text("Recovery Answer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Forgot passcode recovery works only from the lock screen by answering this question.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
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

                if (newPasscode.isNotBlank() && !passcodesMatch) {
                    Text(
                        text = "New passcodes must match and contain at least 4 digits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                if (localError.isNotBlank()) {
                    Text(
                        text = localError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val updated = onSave(
                            currentPasscode,
                            newPasscode,
                            selectedRecoveryQuestion,
                            recoveryAnswer,
                            useBiometric && biometricAvailable
                        )
                        if (!updated) {
                            localError = "Existing passcode is incorrect."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSave
                ) {
                    Text("Update Passcode")
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    recoveryQuestion: String,
    errorMessage: String,
    onUnlockWithPasscode: (String) -> Boolean,
    onUnlockWithBiometric: (() -> Unit)?,
    onResetWithRecovery: ((recoveryAnswer: String, newPasscode: String, enableBiometric: Boolean) -> Boolean)? = null
) {
    var passcode by remember { mutableStateOf("") }
    var showRecoveryFlow by remember(recoveryQuestion) { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }
    var confirmNewPasscode by remember { mutableStateOf("") }
    var useBiometric by remember(biometricAvailable, biometricEnabled) {
        mutableStateOf(biometricAvailable && biometricEnabled)
    }
    var localError by remember { mutableStateOf("") }
    val recoveryAvailable = recoveryQuestion.isNotBlank() && onResetWithRecovery != null
    val recoveryPasscodesMatch = newPasscode.length >= 4 && newPasscode == confirmNewPasscode

    CredFlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Dafira logo",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Dafira Locked",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (showRecoveryFlow) {
                    "Answer your saved recovery question to reset the passcode."
                } else {
                    "Enter your passcode or verify with biometrics to continue."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            FlowCard(
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showRecoveryFlow) {
                    Text(
                        text = recoveryQuestion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = {
                            recoveryAnswer = it
                            localError = ""
                        },
                        label = { Text("Recovery Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPasscode,
                        onValueChange = {
                            newPasscode = it.take(6)
                            localError = ""
                        },
                        label = { Text("New Passcode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmNewPasscode,
                        onValueChange = {
                            confirmNewPasscode = it.take(6)
                            localError = ""
                        },
                        label = { Text("Confirm New Passcode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (biometricAvailable) {
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
                                    text = "Enable biometric unlock after the reset completes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useBiometric,
                                onCheckedChange = { useBiometric = it }
                            )
                        }
                    }

                    if (newPasscode.isNotBlank() && !recoveryPasscodesMatch) {
                        Text(
                            text = "New passcodes must match and contain at least 4 digits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val reset = onResetWithRecovery?.invoke(
                                recoveryAnswer,
                                newPasscode,
                                useBiometric && biometricAvailable
                            ) == true
                            if (!reset) {
                                localError = "Recovery answer is incorrect."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = recoveryAnswer.isNotBlank() && recoveryPasscodesMatch
                    ) {
                        Text("Reset Passcode")
                    }
                    TextButton(
                        onClick = {
                            showRecoveryFlow = false
                            localError = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Unlock")
                    }
                } else {
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

                    if (recoveryAvailable) {
                        TextButton(
                            onClick = {
                                showRecoveryFlow = true
                                localError = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Forgot Passcode?")
                        }
                    }
                }

                val message = localError.ifBlank {
                    if (showRecoveryFlow) "" else errorMessage
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryQuestionDropdown(
    questions: List<String>,
    selectedQuestion: String,
    onQuestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedQuestion,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Recovery Question") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            questions.forEach { question ->
                DropdownMenuItem(
                    text = { Text(question) },
                    onClick = {
                        expanded = false
                        onQuestionSelected(question)
                    }
                )
            }
        }
    }
}
