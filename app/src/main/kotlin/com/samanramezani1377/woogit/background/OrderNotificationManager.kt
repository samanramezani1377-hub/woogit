package com.samanramezani1377.woogit.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.samanramezani1377.woogit.R

class OrderNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "new_orders"
        const val EXTRA_STORE_ID = "store_id"
        const val EXTRA_ORDER_ID = "order_id"
        private const val CHANNEL_NAME = "New orders"
    }

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun notify(order: BackgroundOrder) {
        ensureChannel()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(EXTRA_STORE_ID, order.storeId)
            putExtra(EXTRA_ORDER_ID, order.orderId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        } ?: return
        val pending = PendingIntent.getActivity(
            context,
            order.orderId.hashCode(),
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
        NotificationManagerCompat.from(context).notify(order.orderId.hashCode(), notification)
    }
}
