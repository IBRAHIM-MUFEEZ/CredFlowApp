# Share Statement Feature

## Overview
Added a comprehensive statement sharing feature that allows users to generate and share PDF statements containing customer transaction details and EMI schedules.

## Files Created/Modified

### New Files
1. **StatementGenerator.kt** (`app/src/main/java/com/radafiq/data/backup/StatementGenerator.kt`)
   - Generates PDF statements with customer details
   - Includes transaction history with status indicators
   - Shows EMI schedule with instalment breakdown
   - Uses Android's native PdfDocument API (no external dependencies)

2. **file_paths.xml** (`app/src/main/res/xml/file_paths.xml`)
   - FileProvider configuration for sharing PDFs
   - Allows cache directory access for generated statements

### Modified Files
1. **CustomersScreen.kt**
   - Added Share icon button to CustomerDetailScreen top bar
   - Added `ShareStatementDialog` composable for statement generation UI
   - Integrated statement generation with share intent
   - Added `rememberCoroutineScope` import for async operations

2. **AndroidManifest.xml**
   - Added FileProvider configuration for secure file sharing

3. **build.gradle.kts**
   - No new dependencies needed (uses Android framework APIs)

## Features

### Statement Contents
- **Header**: Customer name, statement date
- **Summary Section**: 
  - Total Used amount
  - Customer Paid amount
  - Current Balance
- **Transactions Section**:
  - Transaction date
  - Transaction name and account
  - Amount and status (Settled/Pending/Partial)
  - Sorted by date (newest first)
- **EMI Schedule** (if applicable):
  - Grouped by EMI plan
  - Shows instalment number and date
  - Amount for each instalment

### User Flow
1. Open customer detail screen
2. Tap the Share icon (top right)
3. Dialog shows statement preview with customer summary
4. Tap "Generate & Share" button
5. PDF is generated and system share dialog opens
6. User can share via email, messaging, cloud storage, etc.

### Technical Details
- **PDF Generation**: Uses Android's `PdfDocument` API (API 19+)
- **File Storage**: Generated PDFs stored in app cache directory
- **File Sharing**: Uses `FileProvider` for secure URI sharing
- **Async Processing**: Statement generation runs on IO dispatcher
- **Error Handling**: User-friendly error messages if generation fails

## PDF Layout
- A4 page size (595x842 points)
- Multi-page support (auto-creates new pages when content exceeds page height)
- Professional formatting with:
  - Bold headers and titles
  - Organized sections with spacing
  - Box-styled summary metrics
  - Footer with page numbers

## Usage Example
```kotlin
// In CustomerDetailScreen
if (showShareDialog) {
    ShareStatementDialog(
        customer = customer,
        onDismiss = { showShareDialog = false }
    )
}
```

## Share Destinations
Users can share the generated PDF to:
- Email clients (Gmail, Outlook, etc.)
- Cloud storage (Google Drive, OneDrive, Dropbox)
- Messaging apps (WhatsApp, Telegram)
- File managers
- Any app that accepts PDF files

## Future Enhancements
- Add custom branding/logo to PDF
- Include payment terms and notes
- Add QR code for quick reference
- Support for multiple statement formats
- Email integration for direct sending
- Statement archival/history
