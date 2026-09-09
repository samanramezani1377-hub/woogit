package com.samanramezani1377.woogit.presentation.update

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold

@Composable
internal fun ForceUpdateScreen(updateUrl: String) {
    val context = LocalContext.current
    BackHandler(enabled = true) {}
    GlassScaffold {
        Box(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("نسخه شما منسوخ شده است", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("برای ادامه استفاده از WooGit باید اپ را به آخرین نسخه بروزرسانی کنید.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                GlassPrimaryAction(
                    label = "بروزرسانی",
                    onClick = {
                        val uri = runCatching { Uri.parse(updateUrl) }.getOrNull() ?: return@GlassPrimaryAction
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = updateUrl.isNotBlank(),
                )
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://woogit.ir"))) }) { Text("ورود به woogit.ir") }
            }
        }
    }
}
