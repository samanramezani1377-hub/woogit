package com.samanramezani1377.woogit.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class WooGitNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "orders"
        const val ORDER_ID = "order_id"
        const val STORE_ID = "store_id"
        private const val CHANNEL_NAME = "سفارش‌ها"
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun showOrder(storeId: String, orderId: Long, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(localizeNotificationText(title))
            .setContentText(localizeNotificationText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(storeId, orderId), notification)
    }

    private fun notificationId(storeId: String, orderId: Long): Int = "$storeId:$orderId".hashCode()
}

private fun localizeNotificationText(value: String): String {
    val text = value.trim()
    return when {
        text.startsWith("New order #", ignoreCase = true) -> "سفارش جدید #${text.substringAfter('#').toPersianDigits()}"
        text.equals("New order", ignoreCase = true) -> "سفارش جدید"
        text.equals("Order updated", ignoreCase = true) -> "سفارش به‌روزرسانی شد"
        text.equals("Order completed", ignoreCase = true) -> "سفارش تکمیل شد"
        text.equals("Order cancelled", ignoreCase = true) -> "سفارش لغو شد"
        text.equals("Payment failed", ignoreCase = true) -> "پرداخت ناموفق بود"
        text.equals("Sync completed", ignoreCase = true) -> "همگام‌سازی با موفقیت انجام شد"
        text.equals("Sync failed", ignoreCase = true) -> "همگام‌سازی ناموفق بود"
        text.equals("Connection failed", ignoreCase = true) -> "ارتباط با فروشگاه برقرار نشد"
        else -> text.toPersianDigits()
    }
}

private fun String.toPersianDigits(): String = map { ch -> if (ch in '0'..'9') ('۰'.code + ch.code - '0'.code).toChar() else ch }.joinToString("")
