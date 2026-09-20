package com.samanramezani1377.woogit.bazaar

import android.content.Context
import com.samanramezani1377.woogit.BuildConfig
import com.samanramezani1377.woogit.core.billing.BillingActivation
import com.samanramezani1377.woogit.core.billing.BillingGateway
import com.samanramezani1377.woogit.core.billing.BillingPurchase
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BazaarBillingClient(context: Context, private val backend: BillingGateway) {
    private val payment = Payment(
        context = context.applicationContext,
        config = PaymentConfiguration(
            localSecurityCheck = if (BuildConfig.BAZAAR_RSA_PUBLIC_KEY.isBlank()) SecurityCheck.Disable
            else SecurityCheck.Enable(BuildConfig.BAZAAR_RSA_PUBLIC_KEY),
            shouldSupportSubscription = true,
        ),
    )
    @Volatile private var connection: ir.cafebazaar.poolakey.Connection? = null

    private suspend fun ensureConnected() {
        if (connection != null) return
        suspendCancellableCoroutine<Unit> { continuation ->
            val currentConnection = payment.connect {
                connectionSucceed {
                    connection = currentConnection
                    if (continuation.isActive) continuation.resume(Unit)
                }
                connectionFailed { error ->
                    connection = null
                    if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                }
                disconnected { connection = null }
            }
            connection = currentConnection
            continuation.invokeOnCancellation {
                currentConnection.disconnect()
                connection = null
            }
        }
    }

    suspend fun purchase(storeId: StoreId, productId: String): Result<BillingActivation> = runCatching {
        require(productId.isNotBlank()) { "bazaar_product_not_configured" }
        val registry = BazaarBillingRuntime.registry ?: error("bazaar_activity_registry_unavailable")
        ensureConnected()
        val request = PurchaseRequest(
            productId = productId,
            payload = "woogit:" + storeId.value + ":" + System.currentTimeMillis(),
        )
        val purchase = suspendCancellableCoroutine<ir.cafebazaar.poolakey.entity.PurchaseInfo> { continuation ->
            payment.subscribeProduct(registry, request) {
                purchaseSucceed { info -> if (continuation.isActive) continuation.resume(info) }
                purchaseCanceled { if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("bazaar_purchase_canceled"))) }
                purchaseFailed { error -> if (continuation.isActive) continuation.resumeWith(Result.failure(error)) }
                failedToBeginFlow { error -> if (continuation.isActive) continuation.resumeWith(Result.failure(error)) }
            }
        }
        backend.verifyBazaarPurchase(
            storeId,
            BillingPurchase(
                productId = purchase.productId,
                purchaseToken = purchase.purchaseToken,
                orderId = purchase.orderId,
                purchaseTime = purchase.purchaseTime,
                packageName = purchase.packageName,
            ),
        ).getOrThrow()
    }

    suspend fun reconcile(storeId: StoreId): Result<Unit> = runCatching {
        ensureConnected()
        val purchases = suspendCancellableCoroutine<List<ir.cafebazaar.poolakey.entity.PurchaseInfo>> { continuation ->
            payment.getSubscribedProducts {
                querySucceed { if (continuation.isActive) continuation.resume(it) }
                queryFailed { error -> if (continuation.isActive) continuation.resumeWith(Result.failure(error)) }
            }
        }
        purchases.forEach { purchase ->
            backend.verifyBazaarPurchase(storeId, BillingPurchase(purchase.productId, purchase.purchaseToken, purchase.orderId, purchase.purchaseTime, purchase.packageName)).getOrThrow()
        }
    }

    fun disconnect() {
        connection?.disconnect()
        connection = null
    }
}