# Radafiq (CredFlow)

A comprehensive Android financial management application designed to help users track transactions, manage accounts and customers, monitor savings, and analyze financial data. Built natively with Kotlin and Jetpack Compose, the app emphasizes security, seamless cloud synchronization, and offline capabilities.

## 🚀 Features

* **Financial Dashboard & Analytics:** Get a clear overview of your financial health with an intuitive dashboard and detailed analytics screens.
* **Transaction Management:** Easily add and track daily transactions and payments.
* **Customer & Account Tracking:** Manage multiple accounts and keep detailed records of customer-specific transactions.
* **Savings Management:** Monitor and manage your savings goals directly within the app.
* **Robust Security:** Secure your data with Biometric Authentication and Google Sign-In capabilities.
* **Cloud & Local Backups:** Never lose your data. Seamlessly backup and restore your financial records using Google Drive or local JSON file exports.
* **PDF Statement Generation:** Generate, preview, and share detailed financial statements seamlessly.
* **Automated Reminders:** Stay on top of your finances with automated background due date reminders.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Architecture:** MVVM (Model-View-ViewModel)
* **Backend & Database:** Firebase (Firestore & Authentication)
* **Security:** Android Biometric API (`BiometricAuthManager`)
* **Background Tasks:** Android WorkManager (`DueReminderWorker`, `DueReminderScheduler`)
* **Cloud Integration:** Google Drive API (for remote backup/restore functionality)

## 📁 Core Project Structure

* `com.radafiq.data.*`: Contains repositories and models for authentication, backups (Drive & Local), user profiles, settings, and Firebase interactions.
* `com.radafiq.ui.*`: Houses all Jetpack Compose UI screens (`DashboardScreen`, `AccountsScreen`, `AnalyticsScreen`, etc.) and the core design system (`CredFlowTheme`).
* `com.radafiq.viewmodel.*`: Contains the `MainViewModel` for handling business logic and UI state management.
* `com.radafiq.security.*`: Manages biometric and local app security implementations.
* `com.radafiq.reminders.*`: Manages background workers for scheduling and pushing payment/due reminders to the user.

## ⚙️ Getting Started

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Latest version recommended)
* A Firebase Project with Firestore and Google Authentication enabled.

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/ibrahim-mufeez/radafiq.git](https://github.com/ibrahim-mufeez/radafiq.git)
    ```
2.  **Open the project:**
    Open the cloned directory in Android Studio.
3.  **Firebase Setup:**
    * Download your `google-services.json` file from your Firebase console.
    * Place the `google-services.json` file in the `app/` directory.
4.  **Build and Run:**
    Sync the Gradle files and run the application on an emulator or a physical device.

## 🛡️ Security & Privacy

Radafiq prioritizes user privacy by utilizing local biometric prompts and customizable backup serialization (`BackupJsonSerializer`) to ensure that financial data remains securely encrypted and accessible only to the authorized user.
