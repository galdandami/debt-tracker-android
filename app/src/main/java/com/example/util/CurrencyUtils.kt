package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyOption(
    val symbol: String,
    val name: String,
    val code: String
) {
    val displayLabel: String
        get() = "$symbol $name"
}

object CurrencyUtils {

    val popularCurrencies = listOf(
        CurrencyOption("₽", "Рубль", "RUB"),
        CurrencyOption("$", "Доллар", "USD"),
        CurrencyOption("€", "Евро", "EUR"),
        CurrencyOption("₸", "Тенге", "KZT"),
        CurrencyOption("₴", "Гривна", "UAH"),
        CurrencyOption("Br", "Бел. рубль", "BYN"),
        CurrencyOption("£", "Фунт", "GBP")
    )

    fun formatAmount(amount: Double, currencySymbol: String = "₽"): String {
        val symbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }

        val formatter = if (amount % 1.0 == 0.0) {
            DecimalFormat("#,##0", symbols)
        } else {
            DecimalFormat("#,##0.00", symbols)
        }

        val formattedNum = formatter.format(amount)
        return "$formattedNum $currencySymbol".trim()
    }

    fun parseAmount(text: String): Double? {
        val clean = text.replace(" ", "").replace(",", ".")
        return clean.toDoubleOrNull()
    }
}
