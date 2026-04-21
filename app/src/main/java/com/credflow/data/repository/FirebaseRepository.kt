package com.credflow.data.repository
import com.credflow.data.models.CardSummary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    // ✅ SAVE TRANSACTION
    suspend fun addTransaction(
        accountId: String,
        amount: Double,
        givenDate: String,
        dueDate: String
    ) {
        val data = hashMapOf(
            "accountId" to accountId,
            "amount" to amount,
            "givenDate" to givenDate,
            "dueDate" to dueDate
        )

        db.collection("transactions").add(data).await()
    }

    // ✅ SAVE PAYMENT
    suspend fun addPayment(
        accountId: String,
        amount: Double,
        date: String
    ) {
        val data = hashMapOf(
            "accountId" to accountId,
            "amount" to amount,
            "date" to date
        )

        db.collection("payments").add(data).await()
    }

    // ✅ REAL-TIME LISTENER
    fun listenAllData(onResult: (List<CardSummary>) -> Unit) {

        db.collection("accounts").addSnapshotListener { accSnap, _ ->
            val accounts = accSnap?.documents ?: return@addSnapshotListener

            db.collection("transactions").addSnapshotListener { tSnap, _ ->
                db.collection("payments").addSnapshotListener { pSnap, _ ->

                    val result = mutableListOf<CardSummary>()

                    for (acc in accounts) {

                        val type = acc.getString("type") ?: ""
                        if (type != "credit_card") continue

                        val id = acc.id
                        val name = acc.getString("name") ?: ""
                        val bill = acc.getDouble("bill") ?: 0.0

                        var totalGiven = 0.0
                        var totalReceived = 0.0

                        tSnap?.documents?.forEach {
                            if (it.getString("accountId") == id)
                                totalGiven += it.getDouble("amount") ?: 0.0
                        }

                        pSnap?.documents?.forEach {
                            if (it.getString("accountId") == id)
                                totalReceived += it.getDouble("amount") ?: 0.0
                        }

                        val pending = totalGiven - totalReceived
                        var payable = bill - pending
                        if (payable < 0) payable = 0.0

                        result.add(
                            CardSummary(id, name, bill, pending, payable)
                        )
                    }

                    onResult(result)
                }
            }
        }
    }
}