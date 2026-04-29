# Account Detail Feature Implementation

## Overview
Implemented a new account detail screen that mirrors the customer detail pattern. When clicking on a credit card or bank account in the Accounts section, it now opens a dedicated detail screen showing all customers who have used that specific account.

## Changes Made

### 1. AccountsScreen.kt
- **Converted to list-row pattern**: Changed from expandable `AccountCard` components to simple clickable `AccountListRow` components
- **Added `AccountListRow`**: New composable that displays account name, type, and total used amount
- **Added `onOpenAccount` callback**: New parameter to handle navigation when an account is clicked
- **Added `AccountDetailScreen`**: New full-screen composable that shows:
  - Account summary (Total Used, Personal Paid, Balance)
  - Credit card due management section (for credit cards only)
  - List of all customers who have transactions on this account
  - Each customer shows their used amount and due amount for this specific account
- **Added `AccountCustomerRow`**: Displays individual customer info within the account detail view

### 2. DashboardScreen.kt
- **Added `onOpenAccount` parameter**: New callback parameter to handle account navigation
- **Wired to AccountsScreen**: Passes the `onOpenAccount` callback through to `AccountsScreen`

### 3. MainActivity.kt
- **Added import**: `import com.radafiq.ui.AccountDetailScreen`
- **Added navigation callback**: Wired `onOpenAccount` in `DashboardScreen` call to navigate to `accountDetail/{accountId}`
- **Added route**: New `composable("accountDetail/{accountId}")` route that renders `AccountDetailScreen`

## User Experience

### Before
- Accounts section showed expandable cards with all details inline
- No way to see which customers used a specific account
- Credit card due management was embedded in the expandable card

### After
- Accounts section shows a clean list of accounts (similar to customers list)
- Tapping an account opens a dedicated detail screen
- Detail screen shows:
  - Account summary metrics at the top
  - Credit card due management (for credit cards)
  - Complete list of customers who used this account
  - Each customer shows their used amount and due amount for this account
- Consistent navigation pattern with customer detail screens

## Technical Details

### Data Calculation
The `AccountDetailScreen` calculates customer-specific amounts by:
1. Filtering all customer transactions to find those matching the account ID
2. Summing transaction amounts to get "used" amount
3. Calculating "paid" amount from partial payments + settled transactions
4. Computing "due" amount as (used - paid)
5. Sorting customers by due amount (highest first)

### Navigation Flow
```
Dashboard → Accounts Tab → Click Account → AccountDetailScreen
                                              ↓
                                         Back button returns to Accounts
```

## Files Modified
1. `app/src/main/java/com/radafiq/ui/AccountsScreen.kt` - Major refactor + new detail screen
2. `app/src/main/java/com/radafiq/ui/DashboardScreen.kt` - Added onOpenAccount parameter
3. `app/src/main/java/com/radafiq/MainActivity.kt` - Added navigation route and wiring

## Consistency
This implementation maintains consistency with the existing customer detail pattern:
- Same navigation approach (list row → detail screen)
- Similar UI layout and components
- Consistent back button behavior
- Matching visual design with FlowCard, MetricPill, etc.
