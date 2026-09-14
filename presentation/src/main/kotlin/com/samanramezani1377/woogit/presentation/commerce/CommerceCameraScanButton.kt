package com.samanramezani1377.woogit.presentation.commerce

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.samanramezani1377.woogit.commerce.CommerceScannerActivity

@Composable
internal fun CommerceCameraScanButton(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    val latestOnDetected by rememberUpdatedState(onDetected)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringExtra(CommerceScannerActivity.EXTRA_BARCODE)
                ?.takeIf { it.isNotBlank() }
                ?.let(latestOnDetected)
        }
    }

    Button(
        onClick = {
            // The scanner Activity is not exported and intentionally has no
            // external intent-filter, so it must be launched explicitly.
            launcher.launch(Intent(context, CommerceScannerActivity::class.java))
        },
    ) {
        Text("اسکن با دوربین")
    }
}
