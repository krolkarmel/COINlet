package com.coinlet.model

data class UserData(
    val nationality : String = "",
    val phoneNumber : String = "",
    val email : String = "",
    val firstName : String = "",
    val secondName : String = "",
    val lastName : String = "",
    val birthDate : String = "",
    val pesel : String = "",
    val address : com.coinlet.model.Address,
    val password : String = "",
    val isVerified : Boolean = false,
    )

