package com.samanramezani1377.woogit.commerce

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.samanramezani1377.woogit.presentation.WooGitTheme

class CommerceScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WooGitTheme {
                BarcodeScannerScreen(onBarcodeDetected = { value ->
                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_BARCODE, value))
                    finish()
                })
            }
        }
    }

    companion object {
        const val ACTION = "com.samanramezani1377.woogit.COMMERCE_SCANNER"
        const val EXTRA_BARCODE = "commerce_barcode"
    }
}
