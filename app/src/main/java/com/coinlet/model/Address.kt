package com.coinlet.model

import androidx.versionedparcelable.VersionedParcelize

@VersionedParcelize
data class Address(
    val city : String,
    val postalCode : String,
    val street : String,
    val houseNumber : String,
    val country : String,
)
