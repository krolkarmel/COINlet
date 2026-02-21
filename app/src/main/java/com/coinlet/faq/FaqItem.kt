package com.coinlet.app.faq

data class FaqItem(
    val question: String,
    val answer: String,
    var expanded: Boolean = false
)