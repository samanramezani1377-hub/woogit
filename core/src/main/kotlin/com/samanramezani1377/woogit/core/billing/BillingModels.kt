package com.samanramezani1377.woogit.core.billing

import com.samanramezani1377.woogit.core.domain.entity.StoreId

interface BillingGateway {
    suspend fun plans(storeId: StoreId): Result<List<BillingPlan>>
    suspend fun status(storeId: StoreId): Result<BillingStatus>
    suspend fun checkout(storeId: StoreId, planId: Int, variationId: Int = 0): Result<BillingCheckout>
    suspend fun verifyBazaarPurchase(storeId: StoreId, purchase: BillingPurchase): Result<BillingActivation> = Result.failure(UnsupportedOperationException("Bazaar billing is unavailable for this provider"))
    suspend fun activateOperationalSession(storeId: StoreId): Result<BillingActivation>
}

data class BillingPlan(
    val id: Int,
    val key: String,
    val name: String,
    val price: String,
    val regularPrice: String,
    val currency: String,
    val billingPeriod: String,
    val billingInterval: Int,
    val description: String,
    val type: String,
    val requiresVariation: Boolean,
    val variations: List<BillingVariation> = emptyList(),
    val bazaarProductId: String? = null,
)

data class BillingVariation(
    val id: Int,
    val name: String,
    val price: String,
    val regularPrice: String,
    val bazaarProductId: String? = null,
)

data class BillingStatus(
    val status: String,
    val startsAt: String?,
    val expiresAt: String?,
    val capabilities: List<String>,
    val trialUsed: Boolean,
)

data class BillingCheckout(
    val orderId: Int,
    val paymentUrl: String,
    val status: String,
)

data class BillingPurchase(
    val productId: String,
    val purchaseToken: String,
    val orderId: String,
    val purchaseTime: Long,
    val packageName: String,
)

data class BillingActivation(
    val session: String,
    val scope: String,
    val expiresAt: String?,
)
