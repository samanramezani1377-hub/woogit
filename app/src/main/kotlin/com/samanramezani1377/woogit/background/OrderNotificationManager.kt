package com.samanramezani1377.woogit.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class OrderNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "new_orders"
        const val EXTRA_STORE_ID = "store_id"
        const val EXTRA_ORDER_ID = "order_id"
        private const val CHANNEL_NAME = "سفارش‌های جدید"

        fun deepLinkIntent(context: Context, storeId: String, orderId: Long): Intent? =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                putExtra(EXTRA_STORE_ID, storeId)
                putExtra(EXTRA_ORDER_ID, orderId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
    }

    fun ensureChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun notify(order: BackgroundOrder): Boolean {
        ensureChannel()
        val intent = deepLinkIntent(context, order.storeId, order.orderId) ?: return false
        val pending = PendingIntent.getActivity(context, "${order.storeId}:${order.orderId}".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val amount = formatPersianAmount(order.total)
        val orderNumber = order.number.toPersianDigits()
        val currency = when (order.currency.trim().uppercase()) {
            "IRT", "IRR", "تومان" -> "تومان"
            "USD" -> "دلار"
            "EUR" -> "یورو"
            "GBP" -> "پوند"
            "AED" -> "درهم"
            "TRY" -> "لیر"
            "SAR" -> "ریال سعودی"
            "QAR" -> "ریال قطر"
            "KWD" -> "دینار کویت"
            "CAD" -> "دلار کانادا"
            "AUD" -> "دلار استرالیا"
            "CNY" -> "یوان"
            "JPY" -> "ین"
            "" -> ""
            else -> "واحد پول"
        }
        val amountText = if (currency.isBlank()) amount else "$amount $currency"
        val itemsText = order.itemSummary.toPersianDigits().replace("items", "کالا").replace("item", "کالا")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("سفارش جدید #$orderNumber")
            .setContentText("مبلغ: $amountText • $itemsText")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify("${order.storeId}:${order.orderId}".hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

private fun String.toPersianDigits(): String = map { ch -> if (ch in '0'..'9') ('۰'.code + ch.code - '0'.code).toChar() else ch }.joinToString("")

private fun formatPersianAmount(value: String): String {
    val number = value.replace(",", "").replace("٬", "").toBigDecimalOrNull() ?: return value.toPersianDigits()
    val plain = number.stripTrailingZeros().toPlainString()
    val parts = plain.split('.', limit = 2)
    val integer = parts[0].removePrefix("-")
    val grouped = integer.reversed().chunked(3).joinToString("٬").reversed()
    val sign = if (plain.startsWith("-")) "−" else ""
    val fraction = parts.getOrNull(1).orEmpty()
    return (sign + grouped + if (fraction.isNotEmpty()) "٫$fraction" else "").toPersianDigits()
}
