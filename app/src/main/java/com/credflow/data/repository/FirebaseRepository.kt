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
            "creditDueAmount" to 0.0
        )

        db.collection("customers").add(data).await()
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
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        customerName: String,
        amount: Double,
        transactionDate: String,
        dueDate: String
    ) {
        val data = mutableMapOf<String, Any>(
            "customerId" to customerId,
            "accountId" to accountId,
            "accountName" to accountName,
            "accountType" to accountKind.storageValue,
            "customerName" to customerName,
            "amount" to amount,
            "transactionDate" to transactionDate,
            "givenDate" to transactionDate
        )

        if (accountKind == AccountKind.CREDIT_CARD && dueDate.isNotBlank()) {
            data["dueDate"] = dueDate
        }

        db.collection("transactions").add(data).await()
    }

    suspend fun updateTransaction(
        transactionId: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: Double,
        transactionDate: String,
        dueDate: String
    ) {
        val data = mutableMapOf<String, Any>(
            "accountId" to accountId,
            "accountName" to accountName,
            "accountType" to accountKind.storageValue,
            "amount" to amount,
            "transactionDate" to transactionDate,
            "givenDate" to transactionDate
        )

        data["dueDate"] = if (accountKind == AccountKind.CREDIT_CARD && dueDate.isNotBlank()) {
            dueDate
        } else {
            FieldValue.delete()
        }

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
        val customerIdsByName = mutableMapOf<String, String>()

        customers.forEach { customer ->
            val name = customer.getString("name").orEmpty()
            if (name.isBlank()) return@forEach

            customerTotals[customer.id] = RunningCustomerTotal(
                id = customer.id,
                name = name,
                creditDueAmount = customer.getDouble("creditDueAmount")
                    ?: customer.getDouble("dueAmount")
                    ?: 0.0
            )
            customerIdsByName[name.trim().lowercase()] = customer.id
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

            val accountTotal = accountTotals.getOrPut(accountId) {
                RunningAccountTotal(
                    id = accountId,
                    name = accountName,
                    accountKind = accountKind
                )
            }
            accountTotal.totalUsed += amount

            val customerName = transaction.getString("customerName").orEmpty()
            if (customerName.isNotBlank()) {
                val storedCustomerId = transaction.getString("customerId").orEmpty()
                val customerId = storedCustomerId.ifBlank {
                    customerIdsByName[customerName.trim().lowercase()]
                        ?: legacyCustomerId(customerName)
                }

                val customerTotal = customerTotals.getOrPut(customerId) {
                    RunningCustomerTotal(
                        id = customerId,
                        name = customerName,
                    )
                }
                customerTotal.totalAmount += amount

                customerTotal.transactions.add(
                    CustomerTransaction(
                        id = transaction.id,
                        customerId = customerId,
                        accountId = accountId,
                        accountName = accountName,
                        accountKind = accountKind,
                        amount = amount,
                        transactionDate = transaction.getString("transactionDate")
                            ?: transaction.getString("givenDate")
                            ?: "",
                        dueDate = transaction.getString("dueDate").orEmpty()
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
            CustomerSummary(
                id = total.id,
                name = total.name,
                totalAmount = total.totalAmount,
                creditDueAmount = total.creditDueAmount,
                balance = (total.totalAmount - total.creditDueAmount).coerceAtLeast(0.0),
                transactions = total.transactions.sortedByDescending { it.transactionDate }
            )
        }

        return AppData(
            accounts = accountSummaries,
            customers = customerSummaries
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
        val transactions: MutableList<CustomerTransaction> = mutableListOf()
    )

    private fun legacyCustomerId(customerName: String): String {
        return "legacy_${customerName.trim().lowercase().hashCode()}"
    }
}
