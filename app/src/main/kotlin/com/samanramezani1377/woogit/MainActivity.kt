package com.samanramezani1377.woogit

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.samanramezani1377.woogit.presentation.E11ReleaseApp
import com.samanramezani1377.woogit.presentation.WooGitTheme

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        val composition=(application as WooGitApplication).composition
        val storeId=intent.getStringExtra("store_id")
        val orderId=intent.getStringExtra("order_id") ?: intent.getLongExtra("order_id",-1L).takeIf{it>0}?.toString()
        setContent { WooGitTheme { E11ReleaseApp(composition.v1Presentation,orderId) } }
    }
    override fun onNewIntent(intent: android.content.Intent?) { super.onNewIntent(intent); setIntent(intent) }
    private fun requestNotificationPermissionIfNeeded(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),REQUEST_NOTIFICATIONS)}
    private companion object{const val REQUEST_NOTIFICATIONS=1001}
}
