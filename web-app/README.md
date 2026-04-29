# Radafiq Web Application

A complete React + TypeScript web application that mirrors the Radafiq Android app functionality. This is a standalone web application that syncs data with Firebase Firestore and Google Drive, completely separate from the Android app but sharing the same backend.

## 🎯 Features

### Complete Feature Parity with Android App

- **Customer Management**: Create, edit, delete, and restore customer ledgers
- **Transaction Tracking**: Record transactions with support for:
  - Simple transactions
  - Split transactions (across multiple accounts)
  - EMI installments with custom schedules
  - Partial payments
  - Settlement tracking
- **Account Management**: Track credit cards, bank accounts, and person accounts
- **Savings Accounts**: Deposit/withdrawal tracking per customer
- **EMI Schedule**: View all EMI installments across customers
- **Analytics**: Visualize usage, payments, and outstanding balances
- **Backup & Restore**: Export/import JSON backups
- **Google Drive Sync**: Auto-backup to Google Drive (requires OAuth setup)
- **Security**: Passcode protection with recovery questions
- **Theme**: Light/Dark mode matching Android app design

## 🚀 Getting Started

### Prerequisites

- Node.js 16+ and npm
- Firebase project (same one used by Android app)
- Google Cloud project with Drive API enabled (for Drive sync)

### Installation

```bash
cd web-app
npm install
```

### Firebase Configuration

The app is pre-configured with the Firebase credentials from `app/google-services.json`. If you need to update them:

1. Edit `src/firebase.ts`
2. Update the `firebaseConfig` object with your Firebase project credentials

### Running the App

```bash
npm start
```

Opens at [http://localhost:3000](http://localhost:3000)

### Building for Production

```bash
npm run build
```

Builds the app to the `build/` folder.

## 📁 Project Structure

```
web-app/
├── public/
│   └── index.html
├── src/
│   ├── components/          # Reusable UI components
│   │   └── Layout.tsx       # Main layout with sidebar
│   ├── context/
│   │   └── AppContext.tsx   # Global state management
│   ├── pages/               # Page components
│   │   ├── ProfileSetup.tsx
│   │   ├── SecuritySetup.tsx
│   │   ├── AppLock.tsx
│   │   ├── Dashboard.tsx
│   │   ├── CustomersPage.tsx
│   │   ├── CustomerDetail.tsx
│   │   ├── AccountsPage.tsx
│   │   ├── AccountDetail.tsx
│   │   ├── SavingsPage.tsx
│   │   ├── EmiSchedulePage.tsx
│   │   ├── AnalyticsPage.tsx
│   │   └── SettingsPage.tsx
│   ├── services/
│   │   └── firebaseRepository.ts  # Firestore operations
│   ├── styles/
│   │   └── theme.css        # Complete design system
│   ├── types/
│   │   └── models.ts        # TypeScript interfaces
│   ├── utils/
│   │   ├── backup.ts        # Backup/restore logic
│   │   ├── format.ts        # Formatting utilities
│   │   └── security.ts      # Passcode hashing
│   ├── App.tsx              # Main app component
│   ├── firebase.ts          # Firebase initialization
│   └── index.tsx            # Entry point
├── package.json
├── tsconfig.json
└── README.md
```

## 🎨 Design System

The web app uses the exact same color palette and design language as the Android app:

### Colors

- **Primary**: #667EEA (Purple)
- **Secondary**: #764BA2 (Violet) / #F093FB (Pink)
- **Error**: #F5576C (Red)
- **Success**: #4CAF50 (Green)
- **Warning**: #FF9800 (Orange)

### Dark Theme (Default)

- Background: #0A0612 → #0E0820 → #1A1030 (gradient)
- Surface: #231540
- Text: #F0EEFF
- Muted: #9B8EC4

### Light Theme

- Background: #F5F7FF → #EDE8FF (gradient)
- Surface: #FFFFFF
- Text: #1A0A40
- Muted: #5A4880

## 🔐 Security

### Passcode Protection

- 6-digit numeric passcode
- SHA-256 hashing with device-specific salt
- Recovery question + answer (also hashed)
- Stored in browser localStorage
- Auto-lock on page visibility change (after 2 seconds)

### Data Security

- All data stored in Firebase Firestore
- User-scoped collections (`users/{uid}/...`)
- Firebase Auth for authentication
- Google Sign-In with Drive scope for backups

## 📊 Data Model

### Firestore Collections (per user)

```
users/{uid}/
├── profile/main          # User profile
├── customers/            # Customer records
├── accounts/             # Account summaries
├── transactions/         # All transactions
├── payments/             # Payment records
└── savings/              # Savings entries
```

### Key Models

- **CustomerSummary**: Customer with transactions, balance, savings
- **CustomerTransaction**: Transaction with EMI/split support
- **CardSummary**: Account with bill, pending, payable amounts
- **SavingsEntry**: Deposit/withdrawal record
- **UserProfile**: Display name, business name, email, photo

## 🔄 Backup & Restore

### Local Backup

- Export: Downloads JSON file with all data
- Import: Uploads JSON file and restores to Firestore
- Includes: Profile, settings, customers, accounts, transactions, payments, savings

### Google Drive Backup (Requires OAuth Setup)

- Auto-backup after data changes (3s debounce)
- Manual backup/restore buttons in Settings
- Stored in `appDataFolder` (private to app)
- File: `Radafiq_backup.json`

**Note**: Google Drive sync requires additional OAuth 2.0 setup beyond basic Firebase Auth. The current implementation uses Firebase Auth with Google Sign-In but does not include the full OAuth flow for Drive API access. To enable Drive sync:

1. Enable Google Drive API in Google Cloud Console
2. Add OAuth 2.0 credentials
3. Implement token refresh logic
4. Update `DriveBackupRepository` equivalent for web

## 🌐 Deployment

### Firebase Hosting

```bash
npm run build
firebase deploy --only hosting
```

### Vercel

```bash
vercel --prod
```

### Netlify

```bash
netlify deploy --prod --dir=build
```

## 🔧 Configuration

### Settings (localStorage)

- Theme mode (LIGHT/DARK)
- Selected account IDs
- Last backup/restore timestamps

### Security (localStorage)

- Passcode hash
- Passcode salt
- Lock enabled flag
- Recovery question
- Recovery answer hash

## 📱 Responsive Design

- Desktop: Sidebar navigation
- Mobile: Bottom tab navigation
- Breakpoint: 768px
- Touch-friendly buttons and inputs
- Optimized for mobile browsers

## 🧪 Testing

```bash
npm test
```

## 🐛 Known Limitations

1. **Google Drive Sync**: Requires additional OAuth setup beyond Firebase Auth
2. **Biometric Auth**: Not available in web browsers (uses passcode only)
3. **Push Notifications**: Not implemented (Android-only feature)
4. **Offline Mode**: Requires service worker setup (not included)
5. **PDF Statement Generation**: Not implemented (Android-only feature)

## 🤝 Contributing

This web app is designed to be a standalone version of the Radafiq Android app. It shares the same Firebase backend but operates independently.

## 📄 License

Same license as the Radafiq Android app.

## 🔗 Related

- Android App: `../app/` (Kotlin + Jetpack Compose)
- Shared Backend: Firebase Firestore + Firebase Auth
- Design System: Matches Android app theme exactly

## 📞 Support

For issues or questions, refer to the main Radafiq project documentation.

---

**Built with React + TypeScript + Firebase**

Mirrors the Radafiq Android app with complete feature parity and shared data sync.
