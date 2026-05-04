package com.radafiq.data.repository

import com.radafiq.data.models.AccountKind
import com.radafiq.data.models.AppData
import com.radafiq.data.models.BackupRecord
import com.radafiq.data.models.CardSummary
import com.radafiq.data.models.CustomerSummary
import com.radafiq.data.models.CustomerTransaction
import com.radafiq.data.models.FirestoreBackupPayload
import com.radafiq.data.models.IndianAccountCatalog
import com.radafiq.data.models.SavingsEntry
import com.radafiq.data.models.SavingsType
import com.radafiq.data.auth.LocalIdentityRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addCustomer(name: String): String {
        val data = hashMapOf(
            "name" to name,
            "creditDueAmount" to 0.0,
            "isDeleted" to false
        )
        val ref = customersCollection().add(data).await()
        return ref.id
    }

    suspend fun deleteCustomer(
        customerId: String,
        customerName: String
    ) {
        customersCollection()
            .document(customerId)
            .set(
                mapOf(
                    "name" to customerName,
                    "isDeleted" to true,
                    "deletedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun restoreCustomer(customerId: String) {
        customersCollection()
            .document(customerId)
            .set(
                mapOf(
                    "isDeleted" to false,
                    "deletedAt" to FieldValue.delete()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun permanentlyDeleteCustomer(
        customerId: String,
        customerName: String
    ) {
        val customerRef = customersCollection().document(customerId)
        val transactionSnapshot = transactionsCollection()
            .whereEqualTo("customerId", customerId)
            .get()
            .await()
        val legacyTransactionSnapshot = transactionsCollection()
            .whereEqualTo("customerName", customerName)
            .get()
            .await()

        val batch = db.batch()
        val deletedTransactionIds = mutableSetOf<String>()
        batch.delete(customerRef)
        transactionSnapshot.documents.forEach { transaction ->
            if (deletedTransactionIds.add(transaction.id)) {
                batch.delete(transaction.reference)
            }
        }
        legacyTransactionSnapshot.documents.forEach { transaction ->
            if (deletedTransactionIds.add(transaction.id)) {
                batch.delete(transaction.reference)
            }
        }
        batch.commit().await()
    }

    suspend fun updateCustomerDueAmount(
        customerId: String,
        customerName: String,
        creditDueAmount: Double
    ) {
        customersCollection()
            .document(customerId)
            .set(
                mapOf(
                    "name" to customerName,
                    "creditDueAmount" to creditDueAmount
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun updateCreditCardDue(
        accountId: String,
        accountName: String,
        dueAmount: Double,
        dueDate: String,
        remindersEnabled: Boolean,
        reminderEmail: String,
        reminderWhatsApp: String
    ) {
        accountsCollection()
            .document(accountId)
            .set(
                mapOf(
                    "name" to accountName,
                    "accountType" to AccountKind.CREDIT_CARD.storageValue,
                    "type" to AccountKind.CREDIT_CARD.storageValue,
                    "dueAmount" to dueAmount,
                    "dueDate" to dueDate,
                    "remindersEnabled" to remindersEnabled,
                    "reminderEmail" to reminderEmail,
                    "reminderWhatsApp" to reminderWhatsApp
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun toggleTransactionSettled(
        transactionId: String,
        isSettled: Boolean,
        settledDate: String
    ) {
        transactionsCollection()
            .document(transactionId)
            .set(
                mapOf(
                    "isSettled" to isSettled,
                    "settledDate" to if (isSettled) settledDate else FieldValue.delete()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun addTransaction(
        customerId: String,
        transactionName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        customerName: String,
        amount: Double,
        transactionDate: String,
        personName: String = "",
        splitGroupId: String = "",
        dueDate: String = "",
        emiGroupId: String = "",
        emiIndex: Int = 0,
        emiTotal: Int = 0
    ) {
        val data = mutableMapOf<String, Any>(
            "customerId" to customerId,
            "transactionName" to transactionName,
            "accountId" to accountId,
            "accountName" to accountName,
            "accountType" to accountKind.storageValue,
            "customerName" to customerName,
            "amount" to amount,
            "transactionDate" to transactionDate,
            "givenDate" to transactionDate
        )
        if (personName.isNotBlank()) data["personName"] = personName
        if (splitGroupId.isNotBlank()) data["splitGroupId"] = splitGroupId
        if (dueDate.isNotBlank()) data["dueDate"] = dueDate
        if (emiGroupId.isNotBlank()) {
            data["emiGroupId"] = emiGroupId
            data["emiIndex"] = emiIndex
            data["emiTotal"] = emiTotal
        }
        transactionsCollection().add(data).await()
    }

    suspend fun addSplitTransactionsBatch(splits: List<Map<String, Any>>) {
        val batch = db.batch()
        splits.forEach { data ->
            batch.set(transactionsCollection().document(), data)
        }
        batch.commit().await()
    }

    /**
     * Convert a single EMI installment into split entries.
     * Deletes the original transaction and creates new split docs that preserve
     * emiGroupId/emiIndex/emiTotal so the installment still belongs to the EMI plan.
     */
    suspend fun convertEmiInstallmentToSplit(
        originalTransactionId: String,
        splits: List<Map<String, Any>>
    ) {
        val batch = db.batch()
        // Delete the original single-account EMI installment
        batch.delete(transactionsCollection().document(originalTransactionId))
        // Create replacement split docs (each carries emiGroupId etc.)
        splits.forEach { data ->
            batch.set(transactionsCollection().document(), data)
        }
        batch.commit().await()
    }

    suspend fun addEmiTransactionsBatch(instalments: List<Map<String, Any>>) {
        // Firestore batch limit is 500 writes; EMI plans are well within that
        val batch = db.batch()
        instalments.forEach { data ->
            val ref = transactionsCollection().document()
            batch.set(ref, data)
        }
        batch.commit().await()
    }

    suspend fun updateTransaction(
        transactionId: String,
        transactionName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: Double,
        transactionDate: String,
        personName: String = ""
    ) {
        val data = mutableMapOf<String, Any?>(
            "transactionName" to transactionName,
            "accountId" to accountId,
            "accountName" to accountName,
            "accountType" to accountKind.storageValue,
            "amount" to amount,
            "transactionDate" to transactionDate,
            "givenDate" to transactionDate,
            "dueDate" to FieldValue.delete()
        )
        if (personName.isNotBlank()) data["personName"] = personName
        else data["personName"] = FieldValue.delete()

        @Suppress("UNCHECKED_CAST")
        transactionsCollection()
            .document(transactionId)
            .set(data as Map<String, Any>, SetOptions.merge())
            .await()
    }

    suspend fun deleteTransaction(transactionId: String) {
        transactionsCollection().document(transactionId).delete().await()
    }

    suspend fun addPartialPayment(
        transactionId: String,
        amount: Double,
        date: String
    ) {
        transactionsCollection()
            .document(transactionId)
            .set(
                mapOf(
                    "partialPaidAmount" to FieldValue.increment(amount),
                    "lastPartialPaymentDate" to date
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun addPayment(
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: Double,
        date: String
    ) {
        val data = hashMapOf(
            "accountId" to accountId,
            "accountName" to accountName,
            "accountType" to accountKind.storageValue,
            "amount" to amount,
            "date" to date
        )

        paymentsCollection().add(data).await()
    }

    // ── Savings ───────────────────────────────────────────────────────────────

    suspend fun addSavingsEntry(
        customerId: String,
        customerName: String,
        amount: Double,
        type: SavingsType,
        note: String,
        date: String,
        bankAccountId: String = "",
        bankAccountName: String = ""
    ) {
        savingsCollection().add(
            hashMapOf(
                "customerId"      to customerId,
                "customerName"    to customerName,
                "amount"          to amount,
                "type"            to type.storageValue,
                "note"            to note,
                "date"            to date,
                "bankAccountId"   to bankAccountId,
                "bankAccountName" to bankAccountName
            )
        ).await()
    }

    suspend fun deleteSavingsEntry(entryId: String) {
        savingsCollection().document(entryId).delete().await()
    }

    fun listenAllData(onResult: (AppData) -> Unit): List<com.google.firebase.firestore.ListenerRegistration> {
        val root = userRoot()

        var latestCustomers: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
        var latestAccounts: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
        var latestTransactions: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
        var latestPayments: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
        var latestSavings: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

        var customersReady = false
        var accountsReady = false
        var transactionsReady = false
        var paymentsReady = false
        var savingsReady = false

        fun notifyIfReady() {
            if (customersReady && accountsReady && transactionsReady && paymentsReady && savingsReady) {
                onResult(
                    buildAppData(
                        customers = latestCustomers,
                        accounts = latestAccounts,
                        transactions = latestTransactions,
                        payments = latestPayments,
                        savings = latestSavings
                    )
                )
            }
        }

        val r1 = root.collection("customers").addSnapshotListener { snap, _ ->
            latestCustomers = snap?.documents.orEmpty()
            customersReady = true
            notifyIfReady()
        }

        val r2 = root.collection("accounts").addSnapshotListener { snap, _ ->
            latestAccounts = snap?.documents.orEmpty()
            accountsReady = true
            notifyIfReady()
        }

        val r3 = root.collection("transactions").addSnapshotListener { snap, _ ->
            latestTransactions = snap?.documents.orEmpty()
            transactionsReady = true
            notifyIfReady()
        }

        val r4 = root.collection("payments").addSnapshotListener { snap, _ ->
            latestPayments = snap?.documents.orEmpty()
            paymentsReady = true
            notifyIfReady()
        }

        val r5 = root.collection("savings").addSnapshotListener { snap, _ ->
            latestSavings = snap?.documents.orEmpty()
            savingsReady = true
            notifyIfReady()
        }

        return listOf(r1, r2, r3, r4, r5)
    }

    suspend fun exportBackup(
        profile: Map<String, Any?> = emptyMap(),
        settings: Map<String, Any?> = emptyMap()
    ): FirestoreBackupPayload {
        val customers = customersCollection().get().await().documents.map { document ->
            BackupRecord(
                id = document.id,
                fields = mapOf(
                    "name" to document.getString("name").orEmpty(),
                    "creditDueAmount" to (document.getDouble("creditDueAmount") ?: 0.0),
                    "isDeleted" to (document.getBoolean("isDeleted") == true)
                )
            )
        }
        val accounts = accountsCollection().get().await().documents.map { document ->
            BackupRecord(
                id = document.id,
                fields = mapOf(
                    "name" to document.getString("name").orEmpty(),
                    "accountType" to (
                        document.getString("accountType")
                            ?: document.getString("type")
                            ?: AccountKind.CREDIT_CARD.storageValue
                        ),
                    "dueAmount" to (document.getDouble("dueAmount") ?: 0.0),
                    "dueDate" to document.getString("dueDate").orEmpty(),
                    "remindersEnabled" to (document.getBoolean("remindersEnabled") == true),
                    "reminderEmail" to document.getString("reminderEmail").orEmpty(),
                    "reminderWhatsApp" to document.getString("reminderWhatsApp").orEmpty()
                )
            )
        }
        val transactions = transactionsCollection().get().await().documents.map { document ->
            BackupRecord(
                id = document.id,
                fields = mapOf(
                    "customerId" to document.getString("customerId").orEmpty(),
                    "transactionName" to (
                        document.getString("transactionName")
                            ?: document.getString("name")
                            ?: ""
                        ),
                    "accountId" to document.getString("accountId").orEmpty(),
                    "accountName" to document.getString("accountName").orEmpty(),
                    "accountType" to document.getString("accountType").orEmpty(),
                    "customerName" to document.getString("customerName").orEmpty(),
                    "amount" to (document.getDouble("amount") ?: 0.0),
                    "transactionDate" to (
                        document.getString("transactionDate")
                            ?: document.getString("givenDate")
                            ?: ""
                        ),
                    "givenDate" to (
                        document.getString("givenDate")
                            ?: document.getString("transactionDate")
                            ?: ""
                        ),
                    "isSettled" to (document.getBoolean("isSettled") == true),
                    "settledDate" to document.getString("settledDate").orEmpty(),
                    "dueDate" to document.getString("dueDate").orEmpty(),
                    "partialPaidAmount" to (document.getDouble("partialPaidAmount") ?: 0.0),
                    "emiGroupId" to document.getString("emiGroupId").orEmpty(),
                    "emiIndex" to (document.getLong("emiIndex") ?: 0L),
                    "emiTotal" to (document.getLong("emiTotal") ?: 0L),
                    "personName" to document.getString("personName").orEmpty(),
                    "splitGroupId" to document.getString("splitGroupId").orEmpty()
                )
            )
        }
        val payments = paymentsCollection().get().await().documents.map { document ->
            BackupRecord(
                id = document.id,
                fields = mapOf(
                    "accountId" to document.getString("accountId").orEmpty(),
                    "accountName" to document.getString("accountName").orEmpty(),
                    "accountType" to document.getString("accountType").orEmpty(),
                    "amount" to (document.getDouble("amount") ?: 0.0),
                    "date" to document.getString("date").orEmpty()
                )
            )
        }

        val savings = savingsCollection().get().await().documents.map { document ->
            BackupRecord(
                id = document.id,
                fields = mapOf(
                    "customerId"   to document.getString("customerId").orEmpty(),
                    "customerName" to document.getString("customerName").orEmpty(),
                    "amount"       to (document.getDouble("amount") ?: 0.0),
                    "type"         to document.getString("type").orEmpty(),
                    "note"         to document.getString("note").orEmpty(),
                    "date"         to document.getString("date").orEmpty()
                )
            )
        }

        return FirestoreBackupPayload(
            exportedAt = Instant.now().toString(),
            profile = profile,
            settings = settings,
            customers = customers,
            accounts = accounts,
            transactions = transactions,
            payments = payments,
            savings = savings
        )
    }

    suspend fun restoreBackup(payload: FirestoreBackupPayload) {
        restoreBackupAsync(payload).forEach { task -> task.await() }
    }

    fun restoreBackupAsync(payload: FirestoreBackupPayload): List<Task<Void>> {
        return buildPendingWrites(payload).chunked(MAX_BATCH_WRITE_COUNT).map { chunk ->
            val batch = db.batch()
            chunk.forEach { pendingWrite ->
                batch.set(
                    pendingWrite.reference,
                    pendingWrite.fields,
                    SetOptions.merge()
                )
            }
            batch.commit()
        }
    }

    private data class PendingWrite(
        val reference: DocumentReference,
        val fields: Map<String, Any?>
    )

    private fun buildPendingWrites(payload: FirestoreBackupPayload): List<PendingWrite> {
        return buildList {
            payload.customers.forEach { record ->
                add(
                    PendingWrite(
                        reference = customersCollection().document(record.id),
                        fields = record.fields.filterValues { it != null }
                    )
                )
            }

            payload.accounts.forEach { record ->
                add(
                    PendingWrite(
                        reference = accountsCollection().document(record.id),
                        fields = record.fields.filterValues { it != null }
                    )
                )
            }

            payload.transactions.forEach { record ->
                add(
                    PendingWrite(
                        reference = transactionsCollection().document(record.id),
                        fields = record.fields.filterValues { it != null }
                    )
                )
            }

            payload.payments.forEach { record ->
                add(
                    PendingWrite(
                        reference = paymentsCollection().document(record.id),
                        fields = record.fields.filterValues { it != null }
                    )
                )
            }

            payload.savings.forEach { record ->
                add(
                    PendingWrite(
                        reference = savingsCollection().document(record.id),
                        fields = record.fields.filterValues { it != null }
                    )
                )
            }
        }
    }

    private companion object {
        const val MAX_BATCH_WRITE_COUNT = 400
    }

    private fun buildAppData(
        customers: List<DocumentSnapshot>,
        accounts: List<DocumentSnapshot>,
        transactions: List<DocumentSnapshot>,
        payments: List<DocumentSnapshot>,
        savings: List<DocumentSnapshot>
    ): AppData {
        val accountTotals = linkedMapOf<String, RunningAccountTotal>()

        IndianAccountCatalog.all.forEach { option ->
            accountTotals[option.id] = RunningAccountTotal(
                id = option.id,
                name = option.name,
                accountKind = option.accountKind
            )
        }

        accounts.forEach { account ->
            val name = account.getString("name").orEmpty()
            if (name.isBlank()) return@forEach

            val accountKind = AccountKind.fromStorage(
                account.getString("accountType") ?: account.getString("type")
            )
            val accountTotal = accountTotals.getOrPut(account.id) {
                RunningAccountTotal(
                    id = account.id,
                    name = name,
                    accountKind = accountKind
                )
            }
            // Note: totalUsed is accumulated from transactions, not stored on the account doc
            accountTotal.dueAmount = account.getDouble("dueAmount") ?: 0.0
            accountTotal.dueDate = account.getString("dueDate").orEmpty()
            accountTotal.remindersEnabled = account.getBoolean("remindersEnabled") == true
            accountTotal.reminderEmail = account.getString("reminderEmail").orEmpty()
            accountTotal.reminderWhatsApp = account.getString("reminderWhatsApp").orEmpty()
        }

        val customerTotals = linkedMapOf<String, RunningCustomerTotal>()
        val deletedCustomerTotals = linkedMapOf<String, RunningCustomerTotal>()
        val customerIdsByName = mutableMapOf<String, String>()
        val deletedCustomerIds = mutableSetOf<String>()

        customers.forEach { customer ->
            val name = customer.getString("name").orEmpty()
            if (name.isBlank()) return@forEach

            val isDeleted = customer.getBoolean("isDeleted") == true
            val targetMap = if (isDeleted) deletedCustomerTotals else customerTotals

            targetMap[customer.id] = RunningCustomerTotal(
                id = customer.id,
                name = name,
                manualPaidAmount = customer.getDouble("creditDueAmount")
                    ?: customer.getDouble("dueAmount")
                    ?: 0.0,
                isDeleted = isDeleted
            )

            if (isDeleted) {
                deletedCustomerIds.add(customer.id)
            } else {
                customerIdsByName[name.trim().lowercase()] = customer.id
            }
        }

        transactions.forEach { transaction ->
            val accountId = transaction.getString("accountId").orEmpty()
            val amount = transaction.getDouble("amount") ?: 0.0
            val splitGroupId = transaction.getString("splitGroupId").orEmpty()
            // Allow split parts with blank accountId to still be attached to the customer
            if (amount <= 0.0) return@forEach
            if (accountId.isBlank() && splitGroupId.isBlank()) return@forEach

            val fallbackOption = IndianAccountCatalog.optionById(accountId)
            val accountKind = AccountKind.fromStorage(
                transaction.getString("accountType") ?: fallbackOption?.accountKind?.storageValue
            )
            val accountName = transaction.getString("accountName")
                ?: fallbackOption?.name
                ?: accountId

            val customerName = transaction.getString("customerName").orEmpty()
            if (customerName.isNotBlank()) {
                val storedCustomerId = transaction.getString("customerId").orEmpty()
                val customerId = storedCustomerId.ifBlank {
                    customerIdsByName[customerName.trim().lowercase()]
                        ?: legacyCustomerId(customerName)
                }

                val isDeleted = customerId in deletedCustomerIds
                val emiGroupId = transaction.getString("emiGroupId").orEmpty()
                val transactionDateStr = transaction.getString("transactionDate")
                    ?: transaction.getString("givenDate") ?: ""
                val isFutureEmi = isFutureScheduledEmi(
                    transactionDate = transactionDateStr,
                    emiGroupId = emiGroupId
                )

                if (!isDeleted && !isFutureEmi) {
                    // Only bank/credit card transactions affect account totals
                    if (accountKind != AccountKind.PERSON) {
                        val accountTotal = accountTotals.getOrPut(accountId) {
                            RunningAccountTotal(
                                id = accountId,
                                name = accountName,
                                accountKind = accountKind
                            )
                        }
                        accountTotal.totalUsed += amount

                        // Customer partial payments and settlements reduce the outstanding balance
                        val partialPaid = transaction.getDouble("partialPaidAmount") ?: 0.0
                        val isSettledTx = transaction.getBoolean("isSettled") == true
                        when {
                            isSettledTx -> accountTotal.customerPaid += amount
                            partialPaid > 0.0 -> accountTotal.customerPaid += partialPaid
                        }
                    }
                }

                val targetCustomers = if (isDeleted) deletedCustomerTotals else customerTotals
                // Only attach to customers that actually exist — prevents phantom/duplicate entries
                val customerTotal = targetCustomers[customerId] ?: return@forEach
                if (!isFutureEmi) {
                    customerTotal.totalAmount += amount
                }
                val isSettled = transaction.getBoolean("isSettled") == true
                if (isSettled && !isFutureEmi) {
                    customerTotal.settledTransactionAmount += amount
                }

                customerTotal.transactions.add(
                    CustomerTransaction(
                        id = transaction.id,
                        customerId = customerId,
                        name = transaction.getString("transactionName")
                            ?: transaction.getString("name")
                            ?: accountName,
                        accountId = accountId,
                        accountName = accountName,
                        accountKind = accountKind,
                        amount = amount,
                        transactionDate = transaction.getString("transactionDate")
                            ?: transaction.getString("givenDate")
                            ?: "",
                        isSettled = isSettled,
                        settledDate = transaction.getString("settledDate").orEmpty(),
                        partialPaidAmount = transaction.getDouble("partialPaidAmount") ?: 0.0,
                        dueDate = transaction.getString("dueDate").orEmpty(),
                        personName = transaction.getString("personName").orEmpty(),
                        splitGroupId = transaction.getString("splitGroupId").orEmpty(),
                        emiGroupId = transaction.getString("emiGroupId").orEmpty(),
                        emiIndex = (transaction.getLong("emiIndex") ?: 0L).toInt(),
                        emiTotal = (transaction.getLong("emiTotal") ?: 0L).toInt()
                    )
                )
            }
        }

        payments.forEach { payment ->
            val accountId = payment.getString("accountId").orEmpty()
            val amount = payment.getDouble("amount") ?: 0.0
            if (accountId.isBlank() || amount <= 0.0) return@forEach

            val fallbackOption = IndianAccountCatalog.optionById(accountId)
            val accountKind = AccountKind.fromStorage(
                payment.getString("accountType") ?: fallbackOption?.accountKind?.storageValue
            )
            val accountName = payment.getString("accountName")
                ?: fallbackOption?.name
                ?: accountId

            val accountTotal = accountTotals.getOrPut(accountId) {
                RunningAccountTotal(
                    id = accountId,
                    name = accountName,
                    accountKind = accountKind
                )
            }
            accountTotal.totalPaid += amount
        }

        val accountSummaries = accountTotals.values.map { total ->
            // payable = what customers still owe (used - customer paid)
            // pending = owner's personal payments toward the account bill
            val customerOutstanding = (total.totalUsed - total.customerPaid).coerceAtLeast(0.0)
            CardSummary(
                id = total.id,
                name = total.name,
                accountKind = total.accountKind,
                bill = total.totalUsed,
                pending = total.totalPaid,
                payable = customerOutstanding,
                dueAmount = total.dueAmount,
                dueDate = total.dueDate,
                remindersEnabled = total.remindersEnabled,
                reminderEmail = total.reminderEmail,
                reminderWhatsApp = total.reminderWhatsApp
            )
        }

        // Attach savings entries to customers
        savings.forEach { doc ->
            val customerId = doc.getString("customerId").orEmpty()
            val amount = doc.getDouble("amount") ?: 0.0
            if (customerId.isBlank() || amount <= 0.0) return@forEach
            val entry = SavingsEntry(
                id = doc.id,
                customerId = customerId,
                customerName = doc.getString("customerName").orEmpty(),
                amount = amount,
                type = SavingsType.fromStorage(doc.getString("type")),
                note = doc.getString("note").orEmpty(),
                date = doc.getString("date").orEmpty(),
                bankAccountId = doc.getString("bankAccountId").orEmpty(),
                bankAccountName = doc.getString("bankAccountName").orEmpty()
            )
            customerTotals[customerId]?.savingsEntries?.add(entry)
            deletedCustomerTotals[customerId]?.savingsEntries?.add(entry)
        }

        val customerSummaries = customerTotals.values.map { total -> total.toSummary() }
        val deletedCustomerSummaries = deletedCustomerTotals.values.map { total -> total.toSummary() }

        return AppData(
            accounts = accountSummaries,
            customers = customerSummaries,
            deletedCustomers = deletedCustomerSummaries
        )
    }

    private fun RunningCustomerTotal.toSummary(): CustomerSummary {
        // totalAmount already excludes future EMIs (set in buildAppData)
        val visibleTxns = transactions.filter { t ->
            if (!t.isEmi) true else !t.isScheduledForFutureMonth()
        }
        // For settled transactions: count the full amount (partial is subsumed by settlement)
        // For unsettled transactions: count only partial paid amount
        val totalPartialPaid = visibleTxns
            .filter { !it.isSettled }
            .sumOf { it.partialPaidAmount }
        val customerPaidAmount = manualPaidAmount + settledTransactionAmount + totalPartialPaid
        val sortedSavings = savingsEntries.sortedByDescending { it.date }
        val savingsBalance = sortedSavings.sumOf {
            if (it.type == SavingsType.DEPOSIT) it.amount else -it.amount
        }.coerceAtLeast(0.0)
        return CustomerSummary(
            id = id,
            name = name,
            totalAmount = totalAmount,
            creditDueAmount = customerPaidAmount,
            manualPaidAmount = manualPaidAmount,
            settledTransactionAmount = settledTransactionAmount,
            partialPaidAmount = totalPartialPaid,
            balance = (totalAmount - customerPaidAmount).coerceAtLeast(0.0),
            transactions = transactions.sortedWith(
                compareByDescending<CustomerTransaction> { it.transactionDate }
                    .thenByDescending { it.id }
            ),
            isDeleted = isDeleted,
            savingsBalance = savingsBalance,
            savingsEntries = sortedSavings
        )
    }

    private fun userRoot() = db.collection("users")
        .document(LocalIdentityRepository.userId())

    private fun customersCollection() = userRoot()
        .collection("customers")

    private fun accountsCollection() = userRoot()
        .collection("accounts")

    private fun transactionsCollection() = userRoot()
        .collection("transactions")

    private fun paymentsCollection() = userRoot()
        .collection("payments")

    private fun savingsCollection() = userRoot()
        .collection("savings")

    private data class RunningAccountTotal(
        val id: String,
        val name: String,
        val accountKind: AccountKind,
        var totalUsed: Double = 0.0,
        var totalPaid: Double = 0.0,       // owner's personal payments (payments collection)
        var customerPaid: Double = 0.0,    // customer partial + settled payments
        var dueAmount: Double = 0.0,
        var dueDate: String = "",
        var remindersEnabled: Boolean = false,
        var reminderEmail: String = "",
        var reminderWhatsApp: String = ""
    )

    private data class RunningCustomerTotal(
        val id: String,
        val name: String,
        var manualPaidAmount: Double = 0.0,
        var settledTransactionAmount: Double = 0.0,
        var totalAmount: Double = 0.0,
        val transactions: MutableList<CustomerTransaction> = mutableListOf(),
        val savingsEntries: MutableList<SavingsEntry> = mutableListOf(),
        val isDeleted: Boolean = false
    )

    private fun legacyCustomerId(customerName: String): String {
        return "legacy_${customerName.trim().lowercase().hashCode()}"
    }

    private fun isFutureScheduledEmi(
        transactionDate: String,
        emiGroupId: String,
        referenceDate: LocalDate = LocalDate.now()
    ): Boolean {
        if (emiGroupId.isBlank()) return false
        val installmentDate = runCatching { LocalDate.parse(transactionDate) }.getOrNull() ?: return false
        return YearMonth.from(installmentDate).isAfter(YearMonth.from(referenceDate))
    }
}
