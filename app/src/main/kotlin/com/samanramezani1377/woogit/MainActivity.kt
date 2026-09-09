package com.samanramezani1377.woogit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.samanramezani1377.woogit.presentation.E11ReleaseApp
import com.samanramezani1377.woogit.presentation.WooGitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationOrderId = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val forceUpdateUrl = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val announcementScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var announcementCenter: AnnouncementCenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        notificationOrderId.value = intentOrderId(intent)
        val composition = (application as WooGitApplication).composition
        announcementCenter = composition.announcementCenter
        forceUpdateUrl.value = announcementCenter.forceUpdateUrl.value
        observeAnnouncementCenter()
        setContent {
            WooGitTheme {
                E11ReleaseApp(
                    dependencies = composition.v1Presentation,
                    accountSetupGateway = composition.accountSetupGateway,
                    initialOrderId = notificationOrderId.value,
                    forceUpdateUrl = forceUpdateUrl.value,
                    bannerAnnouncements = announcementCenter.bannerAnnouncements,
                    onDismissBanner = announcementCenter::dismissBanner,
                )
            }
        }
        requestNotificationPermissionIfNeeded { syncAnnouncements() }
    }

    override fun onStart() {
        super.onStart()
        announcementCenter.start()
    }

    override fun onStop() {
        announcementCenter.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) syncAnnouncements()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentOrderId(intent)?.let { notificationOrderId.value = it }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) syncAnnouncements()
    }

    override fun onDestroy() {
        announcementScope.cancel()
        super.onDestroy()
    }

    private fun intentOrderId(intent: Intent?): String? = intent?.let {
        it.getStringExtra("order_id") ?: it.getLongExtra("order_id", -1L).takeIf { id -> id > 0L }?.toString()
    }

    private fun requestNotificationPermissionIfNeeded(afterPermissionCheck: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        afterPermissionCheck()
    }

    private fun observeAnnouncementCenter() {
        announcementScope.launch {
            announcementCenter.forceUpdateUrl.collect { url -> forceUpdateUrl.value = url }
        }
    }

    private fun syncAnnouncements() {
        announcementScope.launch { announcementCenter.syncNotifications() }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1001
    }
}
