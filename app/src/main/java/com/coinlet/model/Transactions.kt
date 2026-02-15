package com.coinlet.model

data class Transactions(
    val amount: Double = 0.0,
    val receiverName: String = "",
    val receiverIban: String = "",
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: String = "outgoing"
)
