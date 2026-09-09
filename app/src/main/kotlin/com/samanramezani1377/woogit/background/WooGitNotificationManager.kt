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
        private const val ANNOUNCEMENTS_CHANNEL = "announcements"
        private const val IMPORTANT_CHANNEL = "important"
        private const val UPDATES_CHANNEL = "updates"
        private const val PROMOTIONS_CHANNEL = "promotions"
        private const val CHANNEL_NAME = "سفارش‌ها"
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel(ANNOUNCEMENTS_CHANNEL, "اعلان‌ها", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel(IMPORTANT_CHANNEL, "اعلان‌های مهم", NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel(UPDATES_CHANNEL, "به‌روزرسانی‌ها", NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel(PROMOTIONS_CHANNEL, "پیشنهادها", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun showOrder(storeId: String, orderId: Long, title: String, body: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(localizeNotificationText(title))
            .setContentText(localizeNotificationText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(storeId, orderId), notification)
    }

    fun showAnnouncement(id: String, title: String, body: String, notificationType: Int, requestedChannel: String): Boolean {
        if (!canNotify()) return false
        createChannel()
        val channel = allowedChannel(requestedChannel, notificationType)
        val priority = when (notificationType) {
            2, 3 -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(localizeNotificationText(title))
            .setContentText(localizeNotificationText(body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(localizeNotificationText(body)))
            .setAutoCancel(true)
            .setPriority(priority)
            .build()
        NotificationManagerCompat.from(context).notify("announcement:$id".hashCode(), notification)
        return true
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun allowedChannel(requested: String, notificationType: Int): String = when (requested) {
        ANNOUNCEMENTS_CHANNEL, IMPORTANT_CHANNEL, UPDATES_CHANNEL, PROMOTIONS_CHANNEL -> requested
        else -> when (notificationType) {
            2 -> IMPORTANT_CHANNEL
            3 -> UPDATES_CHANNEL
            4 -> PROMOTIONS_CHANNEL
            else -> ANNOUNCEMENTS_CHANNEL
        }
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