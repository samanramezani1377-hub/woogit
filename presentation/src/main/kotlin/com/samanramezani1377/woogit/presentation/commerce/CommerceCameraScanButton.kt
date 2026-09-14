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

private const val SCANNER_ACTIVITY_CLASS = "com.samanramezani1377.woogit.commerce.CommerceScannerActivity"
private const val EXTRA_BARCODE = "commerce_scanner_barcode"

@Composable
internal fun CommerceCameraScanButton(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    val latestOnDetected by rememberUpdatedState(onDetected)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringExtra(EXTRA_BARCODE)
                ?.takeIf { it.isNotBlank() }
                ?.let(latestOnDetected)
        }
    }

    Button(
        onClick = {
            // The scanner Activity lives in the app module, so presentation
            // must not create a compile-time app -> presentation dependency.
            // setClassName still creates an explicit, package-local launch.
            val intent = Intent().setClassName(context, SCANNER_ACTIVITY_CLASS)
            launcher.launch(intent)
        },
    ) {
        Text("اسکن با دوربین")
    }
}
