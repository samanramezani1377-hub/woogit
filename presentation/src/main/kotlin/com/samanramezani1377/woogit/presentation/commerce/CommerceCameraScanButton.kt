package com.samanramezani1377.woogit.presentation.commerce

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal fun CommerceCameraScanButton(onDetected: (String) -> Unit) {
    val latestOnDetected by rememberUpdatedState(onDetected)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringExtra("commerce_barcode")?.takeIf { it.isNotBlank() }?.let(latestOnDetected)
        }
    }
    Button(onClick = { launcher.launch(Intent("com.samanramezani1377.woogit.COMMERCE_SCANNER")) }) {
        Text("اسکن با دوربین")
    }
}
