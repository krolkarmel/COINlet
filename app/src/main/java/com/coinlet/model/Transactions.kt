package com.coinlet.model

import java.util.Date

data class Transactions(
    val transactionId : String = "",
    val userId : String = "",
    val accountId : String = "",
    val toAccount : String = "",
    val amount : Double = 0.0,
    val currency : String = "PLN",
    val title : String = "",
    val dateTransaction : Date,
    val type : String = "",
)
