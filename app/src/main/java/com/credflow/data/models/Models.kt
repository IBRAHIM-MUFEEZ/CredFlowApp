package com.credflow.data.models

data class CardSummary(
    val id: String,
    val name: String,
    val bill: Double,
    val pending: Double,
    val payable: Double
)

data class Transaction(
    val accountId: String = "",
    val amount: Double = 0.0,
    val givenDate: String = "",
    val dueDate: String = ""
)

data class Payment(
    val accountId: String = "",
    val amount: Double = 0.0,
    val date: String = ""
)