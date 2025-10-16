package com.coinlet.model

data class Accounts(
    val accountId : String = "",
    val userId : String = "",
    val accountNumber : String = "",
    val balance : Double = 0.0,
    val currency : String = "PLN"
)
