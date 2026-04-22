package com.credflow.data.models

enum class AccountKind(
    val storageValue: String,
    val label: String
) {
    BANK_ACCOUNT("bank_account", "Bank Account"),
    CREDIT_CARD("credit_card", "Credit Card");

    companion object {
        fun fromStorage(value: String?): AccountKind {
            return when (value?.lowercase()) {
                "bank_account", "bank", "account" -> BANK_ACCOUNT
                "credit_card", "credit", "card" -> CREDIT_CARD
                else -> CREDIT_CARD
            }
        }
    }
}

data class AccountOption(
    val id: String,
    val name: String,
    val accountKind: AccountKind
)

object IndianAccountCatalog {
    val bankAccounts = listOf(
        AccountOption("bank_sbi", "State Bank of India (SBI)", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_hdfc", "HDFC Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_icici", "ICICI Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_axis", "Axis Bank", AccountKind.BANK_ACCOUNT)
    )

    val creditCards = listOf(
        AccountOption("card_sbi", "SBI CARDS", AccountKind.CREDIT_CARD),
        AccountOption("card_hdfc", "HDFC CREDIT CARD", AccountKind.CREDIT_CARD),
        AccountOption("card_jupiter", "JUPITER CREDIT CARD", AccountKind.CREDIT_CARD)
    )

    val all = bankAccounts + creditCards

    fun optionsFor(accountKind: AccountKind): List<AccountOption> {
        return when (accountKind) {
            AccountKind.BANK_ACCOUNT -> bankAccounts
            AccountKind.CREDIT_CARD -> creditCards
        }
    }

    fun optionById(id: String): AccountOption? {
        return all.firstOrNull { it.id == id }
    }
}

data class CardSummary(
    val id: String,
    val name: String,
    val accountKind: AccountKind,
    val bill: Double,
    val pending: Double,
    val payable: Double,
    val dueAmount: Double = 0.0,
    val dueDate: String = "",
    val remindersEnabled: Boolean = false,
    val reminderEmail: String = "",
    val reminderWhatsApp: String = ""
)

data class CustomerSummary(
    val id: String,
    val name: String,
    val totalAmount: Double,
    val creditDueAmount: Double,
    val manualPaidAmount: Double,
    val settledTransactionAmount: Double,
    val balance: Double,
    val transactions: List<CustomerTransaction>,
    val isDeleted: Boolean = false
)

data class CustomerTransaction(
    val id: String,
    val customerId: String,
    val name: String,
    val accountId: String,
    val accountName: String,
    val accountKind: AccountKind,
    val amount: Double,
    val transactionDate: String,
    val isSettled: Boolean = false,
    val settledDate: String = ""
)

data class AppData(
    val accounts: List<CardSummary>,
    val customers: List<CustomerSummary>,
    val deletedCustomers: List<CustomerSummary>
)

data class Transaction(
    val customerId: String = "",
    val transactionName: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val accountType: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val transactionDate: String = "",
    val givenDate: String = ""
)

data class Payment(
    val accountId: String = "",
    val accountName: String = "",
    val accountType: String = "",
    val amount: Double = 0.0,
    val date: String = ""
)
