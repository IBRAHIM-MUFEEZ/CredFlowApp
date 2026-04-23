package com.radafiq.data.repository

import com.radafiq.data.models.AccountKind
import com.radafiq.data.models.AppData
import com.radafiq.data.models.BackupRecord
import com.radafiq.data.models.CardSummary
import com.radafiq.data.models.CustomerSummary
import com.radafiq.data.models.CustomerTransaction
import com.radafiq.data.models.FirestoreBackupPayload
import com.radafiq.data.models.IndianAccountCatalog
import com.radafiq.data.auth.LocalIdentityRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addCustomer(name: String) {
        val data = hashMapOf(
            "name" to name,
            "creditDueAmount" to 0.0,
            "isDeleted" to false
        )

        customersCollection().add(data).await()
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
        transactionDate: String
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

        transactionsCollection().add(data).await()
    }

    suspend fun updateTransaction(
        transactionId: String,
        transactionName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: Double,
        transactionDate: String
    ) {
        val data = mutableMapOf<String, Any>(
            "transactionName" to transactionName,
            "accountId" to accountId,
            "accountName" to accountName,
            "accountType" to accountKind.storageValue,
            "amount" to amount,
            "transactionDate" to transactionDate,
            "givenDate" to transactionDate,
            "dueDate" to FieldValue.delete()
        )

        transactionsCollection()
            .document(transactionId)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun deleteTransaction(transactionId: String) {
        transactionsCollection().document(transactionId).delete().await()
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

    fun listenAllData(onResult: (AppData) -> Unit) {
        val root = userRoot()

        root.collection("customers").addSnapshotListener { cSnap, _ ->
            val customers = cSnap?.documents.orEmpty()

            root.collection("accounts").addSnapshotListener { accSnap, _ ->
                val accounts = accSnap?.documents.orEmpty()

                root.collection("transactions").addSnapshotListener { tSnap, _ ->
                    root.collection("payments").addSnapshotListener { pSnap, _ ->
                        onResult(
                            buildAppData(
                                customers = customers,
                                accounts = accounts,
                                transactions = tSnap?.documents.orEmpty(),
                                payments = pSnap?.documents.orEmpty()
                            )
                        )
                    }
                }
            }
        }
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
                    "settledDate" to document.getString("settledDate").orEmpty()
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

        return FirestoreBackupPayload(
            exportedAt = Instant.now().toString(),
            profile = profile,
            settings = settings,
            customers = customers,
            accounts = accounts,
            transactions = transactions,
            payments = payments
        )
    }

    suspend fun restoreBackup(payload: FirestoreBackupPayload) {
        val pendingWrites = buildList {
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
        }

        pendingWrites.chunked(MAX_BATCH_WRITE_COUNT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { pendingWrite ->
                batch.set(
                    pendingWrite.reference,
                    pendingWrite.fields,
                    SetOptions.merge()
                )
            }
            batch.commit().await()
        }
    }

    private data class PendingWrite(
        val reference: DocumentReference,
        val fields: Map<String, Any?>
    )

    private companion object {
        const val MAX_BATCH_WRITE_COUNT = 400
    }

    private fun buildAppData(
        customers: List<DocumentSnapshot>,
        accounts: List<DocumentSnapshot>,
        transactions: List<DocumentSnapshot>,
        payments: List<DocumentSnapshot>
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
            accountTotal.totalUsed += account.getDouble("bill") ?: 0.0
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
            if (accountId.isBlank() || amount <= 0.0) return@forEach

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
                if (!isDeleted) {
                    val accountTotal = accountTotals.getOrPut(accountId) {
                        RunningAccountTotal(
                            id = accountId,
                            name = accountName,
                            accountKind = accountKind
                        )
                    }
                    accountTotal.totalUsed += amount
                }

                val targetCustomers = if (isDeleted) deletedCustomerTotals else customerTotals
                val customerTotal = targetCustomers.getOrPut(customerId) {
                    RunningCustomerTotal(
                        id = customerId,
                        name = customerName,
                        isDeleted = isDeleted
                    )
                }
                customerTotal.totalAmount += amount
                val isSettled = transaction.getBoolean("isSettled") == true
                if (isSettled) {
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
                        settledDate = transaction.getString("settledDate").orEmpty()
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
            CardSummary(
                id = total.id,
                name = total.name,
                accountKind = total.accountKind,
                bill = total.totalUsed,
                pending = total.totalPaid,
                payable = (total.totalUsed - total.totalPaid).coerceAtLeast(0.0),
                dueAmount = total.dueAmount,
                dueDate = total.dueDate,
                remindersEnabled = total.remindersEnabled,
                reminderEmail = total.reminderEmail,
                reminderWhatsApp = total.reminderWhatsApp
            )
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
        val customerPaidAmount = manualPaidAmount + settledTransactionAmount
        return CustomerSummary(
            id = id,
            name = name,
            totalAmount = totalAmount,
            creditDueAmount = customerPaidAmount,
            manualPaidAmount = manualPaidAmount,
            settledTransactionAmount = settledTransactionAmount,
            balance = (totalAmount - customerPaidAmount).coerceAtLeast(0.0),
            transactions = transactions.sortedWith(
                compareByDescending<CustomerTransaction> { it.transactionDate }
                    .thenByDescending { it.id }
            ),
            isDeleted = isDeleted
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

    private data class RunningAccountTotal(
        val id: String,
        val name: String,
        val accountKind: AccountKind,
        var totalUsed: Double = 0.0,
        var totalPaid: Double = 0.0,
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
        val isDeleted: Boolean = false
    )

    private fun legacyCustomerId(customerName: String): String {
        return "legacy_${customerName.trim().lowercase().hashCode()}"
    }
}
