package com.radafiq.data.models

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
        AccountOption("bank_axis", "Axis Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_kotak", "Kotak Mahindra Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_pnb", "Punjab National Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_bob", "Bank of Baroda", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_union", "Union Bank of India", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_canara", "Canara Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_idfc_first", "IDFC FIRST Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_indusind", "IndusInd Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_au", "AU Small Finance Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_yes", "YES BANK", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_idbi", "IDBI Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_federal", "Federal Bank", AccountKind.BANK_ACCOUNT),
        AccountOption("bank_rbl", "RBL Bank", AccountKind.BANK_ACCOUNT)
    )

    val creditCards = listOf(
        AccountOption("card_sbi", "SBI Card", AccountKind.CREDIT_CARD),
        AccountOption("card_hdfc", "HDFC Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_icici", "ICICI Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_axis", "Axis Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_kotak", "Kotak Mahindra Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_indusind", "IndusInd Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_idfc_first", "IDFC FIRST Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_amex", "American Express India", AccountKind.CREDIT_CARD),
        AccountOption("card_standard_chartered", "Standard Chartered Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_hsbc", "HSBC Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_au", "AU Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_yes", "YES BANK Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_rbl", "RBL Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_onecard", "OneCard", AccountKind.CREDIT_CARD),
        AccountOption("card_amazon_icici", "Amazon Pay ICICI Card", AccountKind.CREDIT_CARD),
        AccountOption("card_flipkart_axis", "Flipkart Axis Bank Credit Card", AccountKind.CREDIT_CARD),
        AccountOption("card_jupiter", "Jupiter Credit Card", AccountKind.CREDIT_CARD)
    )

    val all = bankAccounts + creditCards

    fun optionsFor(
        accountKind: AccountKind,
        selectedAccountIds: Set<String>? = null
    ): List<AccountOption> {
        val baseOptions = when (accountKind) {
            AccountKind.BANK_ACCOUNT -> bankAccounts
            AccountKind.CREDIT_CARD -> creditCards
        }
        return if (selectedAccountIds == null) {
            baseOptions
        } else {
            baseOptions.filter { it.id in selectedAccountIds }
        }
    }

    fun optionById(id: String): AccountOption? {
        return all.firstOrNull { it.id == id }
    }

    fun availableKinds(selectedAccountIds: Set<String>): List<AccountKind> {
        return AccountKind.values().filter { optionsFor(it, selectedAccountIds).isNotEmpty() }
    }

    fun defaultSelectedAccountIds(): Set<String> {
        return all.map { it.id }.toSet()
    }

    fun sanitizeSelectedAccountIds(selectedAccountIds: Set<String>): Set<String> {
        val knownIds = all.map { it.id }.toSet()
        val filteredIds = selectedAccountIds.filterTo(mutableSetOf()) { it in knownIds }
        return if (filteredIds.isEmpty()) {
            defaultSelectedAccountIds()
        } else {
            filteredIds
        }
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
