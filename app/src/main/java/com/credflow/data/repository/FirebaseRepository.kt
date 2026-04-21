package com.credflow.data.repository
import com.credflow.data.models.AccountKind
import com.credflow.data.models.AppData
import com.credflow.data.models.CardSummary
import com.credflow.data.models.CustomerSummary
import com.credflow.data.models.CustomerTransaction
import com.credflow.data.models.IndianAccountCatalog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun addCustomer(name: String) {
        val data = hashMapOf(
            "name" to name,
            "creditDueAmount" to 0.0,
            "isDeleted" to false
        )

        db.collection("customers").add(data).await()
    }

    suspend fun deleteCustomer(
        customerId: String,
        customerName: String
    ) {
        db.collection("customers")
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
        db.collection("customers")
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
        val customerRef = db.collection("customers").document(customerId)
        val transactionSnapshot = db.collection("transactions")
            .whereEqualTo("customerId", customerId)
            .get()
            .await()
        val legacyTransactionSnapshot = db.collection("transactions")
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
        db.collection("customers")
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

    // ✅ SAVE TRANSACTION
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

        db.collection("transactions").add(data).await()
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

        db.collection("transactions")
            .document(transactionId)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun deleteTransaction(transactionId: String) {
        db.collection("transactions").document(transactionId).delete().await()
    }

    // ✅ SAVE PAYMENT
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

        db.collection("payments").add(data).await()
    }

    // ✅ REAL-TIME LISTENER
    fun listenAllData(onResult: (AppData) -> Unit) {

        db.collection("customers").addSnapshotListener { cSnap, _ ->
            val customers = cSnap?.documents.orEmpty()

            db.collection("accounts").addSnapshotListener { accSnap, _ ->
                val accounts = accSnap?.documents.orEmpty()

                db.collection("transactions").addSnapshotListener { tSnap, _ ->
                    db.collection("payments").addSnapshotListener { pSnap, _ ->
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
            accountTotals.putIfAbsent(
                account.id,
                RunningAccountTotal(
                    id = account.id,
                    name = name,
                    accountKind = accountKind,
                    totalUsed = account.getDouble("bill") ?: 0.0
                )
            )
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
                creditDueAmount = customer.getDouble("creditDueAmount")
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
                            ?: ""
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
                payable = (total.totalUsed - total.totalPaid).coerceAtLeast(0.0)
            )
        }

        val customerSummaries = customerTotals.values.map { total ->
            total.toSummary()
        }

        val deletedCustomerSummaries = deletedCustomerTotals.values.map { total ->
            total.toSummary()
        }

        return AppData(
            accounts = accountSummaries,
            customers = customerSummaries,
            deletedCustomers = deletedCustomerSummaries
        )
    }

    private fun RunningCustomerTotal.toSummary(): CustomerSummary {
        return CustomerSummary(
            id = id,
            name = name,
            totalAmount = totalAmount,
            creditDueAmount = creditDueAmount,
            balance = (totalAmount - creditDueAmount).coerceAtLeast(0.0),
            transactions = transactions.sortedByDescending { it.transactionDate },
            isDeleted = isDeleted
        )
    }

    private data class RunningAccountTotal(
        val id: String,
        val name: String,
        val accountKind: AccountKind,
        var totalUsed: Double = 0.0,
        var totalPaid: Double = 0.0
    )

    private data class RunningCustomerTotal(
        val id: String,
        val name: String,
        var creditDueAmount: Double = 0.0,
        var totalAmount: Double = 0.0,
        val transactions: MutableList<CustomerTransaction> = mutableListOf(),
        val isDeleted: Boolean = false
    )

    private fun legacyCustomerId(customerName: String): String {
        return "legacy_${customerName.trim().lowercase().hashCode()}"
    }
}
