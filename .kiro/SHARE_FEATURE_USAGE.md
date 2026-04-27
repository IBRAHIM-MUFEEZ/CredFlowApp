# Share Statement Feature - Usage Guide

## How to Use

### Step 1: Open Customer Detail
- Navigate to Customers tab
- Tap on any customer to open their detail screen

### Step 2: Access Share Feature
The customer detail screen shows:
```
┌─────────────────────────────────────────┐
│ ← [Customer Name]        [Share] [+] [🗑] │  ← Share button here
│    Balance: ₹X,XXX.XX                   │
├─────────────────────────────────────────┤
│ [Used] [Paid] [Balance]                 │
│ ₹X,XXX  ₹X,XXX  ₹X,XXX                  │
├─────────────────────────────────────────┤
│ Transactions...                         │
└─────────────────────────────────────────┘
```

### Step 3: Generate Statement
Tap the Share icon to open the dialog:
```
┌──────────────────────────────────────┐
│ Share Statement                      │
├──────────────────────────────────────┤
│ Generate a PDF statement with        │
│ customer details, transactions,      │
│ and EMI schedule.                    │
│                                      │
│ ┌────────────────────────────────┐  │
│ │ Customer: John Doe             │  │
│ │ Total Used: ₹50,000.00         │  │
│ │ Balance: ₹15,000.00            │  │
│ │ 12 transaction(s)              │  │
│ └────────────────────────────────┘  │
│                                      │
│ [Cancel]  [Generate & Share]         │
└──────────────────────────────────────┘
```

### Step 4: Share PDF
After tapping "Generate & Share":
1. PDF is generated (shows progress dialog)
2. System share sheet appears
3. Choose destination:
   - Email (Gmail, Outlook, etc.)
   - Cloud storage (Google Drive, Dropbox)
   - Messaging (WhatsApp, Telegram)
   - File manager
   - Any PDF-compatible app

## PDF Statement Format

### Page 1 Header
```
═══════════════════════════════════════════════════════════
                    Customer Statement
                        John Doe
                  Generated: 27 Apr 2026
═══════════════════════════════════════════════════════════
```

### Summary Section
```
┌──────────────────┬──────────────────┬──────────────────┐
│   Total Used     │  Customer Paid   │     Balance      │
│   ₹50,000.00     │   ₹35,000.00     │   ₹15,000.00     │
└──────────────────┴──────────────────┴──────────────────┘
```

### Transactions Section
```
Transactions
─────────────────────────────────────────────────────────
27 Apr 2026    Laptop Purchase (HDFC Card)    ₹25,000.00
               Status: Settled

20 Apr 2026    Office Supplies (SBI Account)  ₹5,000.00
               Status: Pending

15 Apr 2026    Monthly Rent (ICICI Card)      ₹20,000.00
               Status: Partial: ₹10,000.00
```

### EMI Schedule (if applicable)
```
EMI Schedule
─────────────────────────────────────────────────────────
Laptop Purchase - 12 instalments

Instalment 1/12    27 Apr 2026    ₹2,500.00
Instalment 2/12    27 May 2026    ₹2,083.33
Instalment 3/12    27 Jun 2026    ₹2,083.33
...
```

### Footer
```
Page 1 • Radafiq Statement
```

## Features Included

✅ **Customer Information**
- Name and generation date
- Summary of financial status

✅ **Transaction Details**
- Date, description, and amount
- Account information
- Settlement status
- Partial payment tracking

✅ **EMI Schedule**
- Grouped by EMI plan
- Instalment number and date
- Individual amounts

✅ **Professional Formatting**
- Multi-page support
- Clear sections and spacing
- Easy-to-read layout
- Page numbers

✅ **Easy Sharing**
- One-tap share to any app
- Secure file handling
- Automatic cleanup

## Example Scenarios

### Scenario 1: Share with Bank
Customer needs to provide statement to bank for loan application:
1. Open customer detail
2. Tap Share
3. Select Email
4. Send to bank's email address

### Scenario 2: Share with Customer
Send transaction summary to customer:
1. Open customer detail
2. Tap Share
3. Select WhatsApp/Telegram
4. Send to customer's number

### Scenario 3: Archive Statement
Save statement for records:
1. Open customer detail
2. Tap Share
3. Select Google Drive/OneDrive
4. Save to cloud storage

## Technical Notes

- **PDF Size**: Typically 50-200 KB depending on transaction count
- **Generation Time**: Usually < 1 second
- **Storage**: Cached files auto-cleanup after share
- **Compatibility**: Works with any PDF viewer
- **Security**: Uses FileProvider for secure sharing
