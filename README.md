# Radafiq

A full-stack financial ledger platform for tracking customer credit, EMI plans, savings, and account dues — available as a native **Android app** (Kotlin + Jetpack Compose) and a **Progressive Web App** (React + TypeScript), both backed by Firebase Firestore with real-time sync.

🌐 **Web App:** https://radafiq-272f9.web.app

---

## ✨ Features

### Core
- **Customer Ledger** — Track credit given to each customer with full transaction history, running balance, and account-wise breakdown
- **Multi-Account Support** — Manage credit cards and bank accounts; transactions are tagged to specific accounts
- **EMI Plans** — Create instalment schedules with automatic due date calculation; view full amortization table per plan
- **Split Transactions** — Split a single transaction across multiple accounts or people in one step
- **Partial Payments** — Record partial collections against any outstanding transaction
- **Savings Tracker** — Track per-customer savings deposits and withdrawals across bank accounts
- **Analytics** — Account-level and customer-level usage, paid, and outstanding breakdowns

### EMI Intelligence
- Installments appear in the transaction list **20 days before their due date** (not just when the transaction month arrives)
- **Overdue detection** — installments past their due date are flagged with red badges and accent borders
- **Due Soon alerts** — installments due within 20 days show orange warnings with a countdown
- EMI tab stats (Settled / Pending / Upcoming) and progress bar update **immediately** when an installment is marked settled
- Split installments are grouped by `emiIndex` so marking one row settles all parts atomically

### Statements & Export
- Generate a **PDF customer statement** directly from the customer detail screen (web) or share screen (Android)
- Statement includes summary metrics, dues overview, full transaction list, and EMI schedule
- Helvetica font rendering with proper `Rs.` currency prefix

### Security
- **Passcode lock** with PBKDF2 hashing and a recovery question
- **Biometric / Passkey unlock** — Windows Hello, Touch ID, Face ID (WebAuthn on web; BiometricPrompt on Android)
- **Auto-lock** — locks after 5 minutes of inactivity (tab hidden / PC sleep)
- Firestore rules enforce strict per-user data isolation (`userId == request.auth.uid`)
- HTTPS-only; no cleartext traffic

### Backup & Restore
- **JSON export/import** — full backup of all customers, transactions, accounts, payments, and savings
- **Google Drive backup** (Android) — encrypted remote backup via Drive API
- One-tap restore from any backup file

### Notifications & Reminders (Android)
- Background `WorkManager` job checks for upcoming dues daily
- Push notifications for credit card due dates

---

## 🛠️ Tech Stack

### Android
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| Navigation | Navigation Compose |
| Backend | Firebase Firestore + Firebase Auth |
| Auth | Google Sign-In + Biometric API |
| Background | WorkManager |
| Security | AndroidX Security Crypto, PBKDF2 |
| Image loading | Coil |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

### Web App
| Layer | Technology |
|---|---|
| Language | TypeScript |
| UI | React 18 + custom CSS |
| Routing | React Router v6 |
| Backend | Firebase Firestore + Firebase Auth |
| Auth | Google Sign-In (popup + redirect) + WebAuthn |
| PDF | jsPDF 4 |
| Build | Vite 6 |
| Hosting | Firebase Hosting |

---

## 📁 Project Structure

```
Radafiq/
├── app/                          # Android app
│   └── src/main/java/com/radafiq/
│       ├── data/
│       │   ├── auth/             # Google Sign-In, local identity
│       │   ├── backup/           # Drive backup, file backup, statement generator
│       │   ├── models/           # Data models
│       │   ├── profile/          # User profile repository
│       │   ├── repository/       # Firebase Firestore repository
│       │   ├── security/         # App security (passcode, biometric)
│       │   └── settings/         # App settings
│       │
│       ├── reminders/            # WorkManager due reminder scheduler
│       ├── security/             # BiometricAuthManager
│       │
│       ├── ui/                   # Jetpack Compose screens
│       │   ├── DashboardScreen
│       │   ├── CustomersScreen
│       │   ├── AccountsScreen
│       │   ├── AnalyticsScreen
│       │   ├── SavingsScreen
│       │   └── SettingsScreen
│       │
│       └── viewmodel/
│           └── MainViewModel
│
├── web-app/                      # Progressive Web App
│   └── src/
│       ├── components/
│       ├── context/
│       ├── pages/
│       │   ├── Dashboard
│       │   ├── CustomersPage
│       │   ├── CustomerDetail
│       │   ├── AccountsPage
│       │   ├── AccountDetail
│       │   ├── EmiSchedulePage
│       │   ├── AnalyticsPage
│       │   ├── SavingsPage
│       │   └── SettingsPage
│       │
│       ├── services/
│       │   └── firebaseRepository.ts
│       │
│       ├── types/
│       │   └── models.ts
│       │
│       └── utils/
│           ├── format.ts
│           └── statementGenerator.ts
```

## ⚙️ Setup

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Hedgehog or later) — for Android
- Node.js 18+ and npm — for web app
- A Firebase project with **Firestore** and **Google Authentication** enabled

### Firebase Setup (both platforms)

1. Go to [Firebase Console](https://console.firebase.google.com) → create a project
2. Enable **Authentication → Google** sign-in provider
3. Enable **Firestore Database** in production mode
4. Apply the security rules from `firestore.rules`:
rules_version = '2'; service cloud.firestore { match /databases/{database}/documents { match /users/{userId}/{document=**} { allow read, write: if request.auth != null && request.auth.uid == userId; } } }


⚙️ Setup
📱 Android
# 1. Clone repository
git clone https://github.com/IBRAHIM-MUFEEZ/Radafiq.git
cd Radafiq
# 2. Add Firebase config
Download google-services.json from Firebase Console
Place it at:
app/google-services.json
# 3. Open in Android Studio
- Sync Gradle
- Run on device or emulator (API 24+)

🌐 Web App
# Navigate to web app
cd web-app

# 1. Install dependencies
npm install

# 2. Create environment file
cp .env.example .env.local
# Add your Firebase config in .env.local

VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
VITE_GOOGLE_WEB_CLIENT_ID=...
# 3. Run locally
npm run dev

# 4. Build for production
npm run build

# 5. Deploy to Firebase Hosting
npx firebase deploy --only hosting
🔐 Security Notes
Firestore data is scoped to the authenticated user's UID
Passcodes are hashed using PBKDF2 + random salt and stored locally only
Biometric / Passkey authentication:
Web → WebAuthn
Android → BiometricPrompt
Web app enforces:
HTTPS-only (Firebase Hosting)
Strict Content Security Policy (CSP)
Android enforces:
usesCleartextTraffic="false"
Secure network security configuration
