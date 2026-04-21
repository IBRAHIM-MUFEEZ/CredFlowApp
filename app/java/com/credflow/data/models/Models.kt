package com.credflow.data.models

data class Account(
    val id: String = "",
    val name: String = "",
    val type: String = "", // credit_card / bank
    val bill: Double = 0.0
)

data class Transaction(
    val accountId: String = "",
    val amount: Double = 0.0
)

data class Payment(
    val accountId: String = "",
    val amount: Double = 0.0
)

data class CardSummary(
    val id: String,
    val name: String,
    val bill: Double,
    val pending: Double,
    val payable: Double
)