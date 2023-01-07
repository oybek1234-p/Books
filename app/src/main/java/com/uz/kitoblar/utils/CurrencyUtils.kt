package com.uz.kitoblar.utils

import java.text.DecimalFormat

enum class Currency(val currency: String) {
    USZ("UZS"),
    USD("USD")
}

var dollarRate = 0.000093
val formatter = DecimalFormat("#,###")
val currency = Currency.USZ

fun formatCurrency(string: String?): String {
    return try {
        if (string == null) return ""
        formatCurrency(string.toLong())
    } catch (e: Exception) {
        ""
    }
}

fun formatCurrency(long: Long): String {
    return if (long == 0L)
        "Bepul"
    else formatter.format(
        if (currency == Currency.USZ) long
        else (long.toDouble() * dollarRate)
    ) + " ${currency.currency}"
}