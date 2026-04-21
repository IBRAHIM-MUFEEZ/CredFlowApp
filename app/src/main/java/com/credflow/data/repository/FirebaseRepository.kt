package com.credflow.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getAccounts() =
        db.collection("accounts").get().await()

    suspend fun getTransactions() =
        db.collection("transactions").get().await()

    suspend fun getPayments() =
        db.collection("payments").get().await()
}