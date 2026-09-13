package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.samanramezani1377.woogit.core.billing.BillingGateway
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun AppBillingMonitor(
    storeId: String?,
    gateway: BillingGateway?,
    onBillingLockedChanged: (Boolean) -> Unit,
) {
    LaunchedEffect(storeId, gateway) {
        while (isActive) {
            if (storeId == null || gateway == null) {
                onBillingLockedChanged(false)
                break
            }

            try {
                gateway.status(StoreId(storeId)).onSuccess { status ->
                    onBillingLockedChanged(status.status !in setOf("active", "trial"))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Network/transport failures must never be interpreted as an expired subscription.
                // Keep the last known billing state and let the next poll retry.
            }

            delay(BILLING_STATUS_REFRESH_MS)
        }
    }
}

private const val BILLING_STATUS_REFRESH_MS = 30_000L
