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
        private const val CHANNEL_NAME = "New orders"

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

    /** Returns false when Android notification permission/settings prevent delivery. */
    fun notify(order: BackgroundOrder): Boolean {
        ensureChannel()
        val intent = deepLinkIntent(context, order.storeId, order.orderId) ?: return false
        val pending = PendingIntent.getActivity(
            context,
            "${order.storeId}:${order.orderId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New order #${order.number}")
            .setContentText("${order.total} • ${order.itemSummary}")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(order.orderId.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
