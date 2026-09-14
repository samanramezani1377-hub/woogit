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
import com.samanramezani1377.woogit.commerce.CommerceScannerActivity

@Composable
internal fun CommerceCameraScanButton(onDetected: (String) -> Unit) {
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
            // Use an explicit Intent. The scanner Activity is intentionally
            // not exported and therefore has no external ACTION intent-filter.
            launcher.launch(Intent(context = androidx.compose.ui.platform.LocalContext.current, CommerceScannerActivity::class.java))
        },
    ) {
        Text("اسکن با دوربین")
    }
}
