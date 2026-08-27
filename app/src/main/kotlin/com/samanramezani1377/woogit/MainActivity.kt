package com.samanramezani1377.woogit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.samanramezani1377.woogit.presentation.E11ReleaseApp
import com.samanramezani1377.woogit.presentation.WooGitTheme

class MainActivity : ComponentActivity() {
    private val notificationOrderId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationOrderId.value = intentOrderId(intent)
        requestNotificationPermissionIfNeeded()
        val composition = (application as WooGitApplication).composition
        setContent {
            WooGitTheme {
                E11ReleaseApp(composition.v1Presentation, notificationOrderId.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            intentOrderId(intent)?.let { notificationOrderId.value = it }
        }
    }

    private fun intentOrderId(intent: Intent?): String? = intent?.let {
        it.getStringExtra("order_id")
            ?: it.getLongExtra("order_id", -1L).takeIf { id -> id > 0L }?.toString()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private companion object { const val REQUEST_NOTIFICATIONS = 1001 }
}
