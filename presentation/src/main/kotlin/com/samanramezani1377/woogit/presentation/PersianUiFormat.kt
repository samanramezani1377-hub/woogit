package com.samanramezani1377.woogit.presentation

import java.math.BigDecimal
import java.math.RoundingMode

/** Shared Persian presentation formatting. Domain/network values remain unchanged. */
fun String.toPersianDigits(): String = map { ch ->
    when (ch) {
        in '0'..'9' -> ('۰'.code + (ch.code - '0'.code)).toChar()
        ',' -> '٬'
        '.' -> '٫'
        else -> ch
    }
}.joinToString("")

fun Number.toPersianNumber(): String = toString().toPersianDigits()

fun String.toPersianFormattedNumber(): String {
    val raw = trim().replace(",", "").replace("٬", "")
    val negative = raw.startsWith('-')
    val unsigned = raw.removePrefix("-")
    val parts = unsigned.split('.', limit = 2)
    val integer = parts.firstOrNull().orEmpty().filter(Char::isDigit)
    if (integer.isEmpty()) return toPersianDigits()
    val grouped = integer.reversed().chunked(3).joinToString("٬").reversed()
    val fraction = parts.getOrNull(1)?.filter(Char::isDigit).orEmpty()
    val result = buildString {
        if (negative) append('−')
        append(grouped)
        if (fraction.isNotEmpty()) append('٫').append(fraction)
    }
    return result.toPersianDigits()
}

fun String.toPersianPrice(): String {
    val raw = trim().replace(",", "").replace("٬", "")
    val value = raw.toBigDecimalOrNull() ?: return toPersianDigits()
    val normalized = value.stripTrailingZeros()
    return normalized.toPlainString().toPersianFormattedNumber()
}

fun Double.toPersianQuantity(): String =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().toPersianFormattedNumber()
