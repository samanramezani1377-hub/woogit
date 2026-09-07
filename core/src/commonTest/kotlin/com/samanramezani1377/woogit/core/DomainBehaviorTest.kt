package com.samanramezani1377.woogit.core

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.EntityTimestamp
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.error.presentationKey
import com.samanramezani1377.woogit.core.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DomainBehaviorTest {
    @Test
    fun everyDomainErrorHasStablePresentationKey() {
        val errors = listOf(
            DomainError.Validation("x") to "validation",
            DomainError.NotFound("product", "1") to "not_found",
            DomainError.Conflict("x") to "conflict",
            DomainError.Network("x") to "network",
            DomainError.Authentication("x") to "authentication",
            DomainError.Permission("x") to "permission",
            DomainError.RateLimited("x") to "rate_limited",
            DomainError.Server("x") to "server",
            DomainError.Unknown("x") to "unknown",
        )
        errors.forEach { (error, key) -> assertEquals(key, error.presentationKey()) }
    }

    @Test
    fun recoverabilityMatchesRetrySemantics() {
        assertFalse(DomainError.Validation("x").recoverable)
        assertFalse(DomainError.NotFound("x", "1").recoverable)
        assertTrue(DomainError.Conflict("x").recoverable)
        assertTrue(DomainError.Network("x").recoverable)
        assertFalse(DomainError.Authentication("x").recoverable)
        assertFalse(DomainError.Permission("x").recoverable)
        assertTrue(DomainError.RateLimited("x").recoverable)
        assertTrue(DomainError.Server("x").recoverable)
        assertFalse(DomainError.Unknown("x").recoverable)
    }

    @Test
    fun coreResultFoldDispatchesExactlyOneBranch() {
        val success = CoreResult.Success(42).fold(
            onSuccess = { value: Int -> value + 1 },
            onFailure = { error: DomainError -> error("failure branch: $error") },
        )
        val failure = CoreResult.Failure(DomainError.Conflict("x")).fold(
            onSuccess = { error("success branch") },
            onFailure = { domainError: DomainError -> domainError.presentationKey() },
        )
        assertEquals(43, success)
        assertEquals("conflict", failure)
    }

    @Test
    fun identifiersAndCredentialReferencesRejectBlankValues() {
        assertFailsWith<IllegalArgumentException> { EntityId(" ") }
        assertFailsWith<IllegalArgumentException> { StoreId("") }
        assertFailsWith<IllegalArgumentException> { CredentialReference(" ") }
        assertEquals("cred-1", CredentialReference("cred-1").value)
    }

    @Test
    fun storeConnectionValidationRequiresHttps() {
        assertTrue(StoreConnection(StoreId("store-1"), "https://shop.example", ConnectionState.CONNECTED, null).validate())
        assertFalse(StoreConnection(StoreId("store-1"), "http://shop.example", ConnectionState.CONNECTED, null).validate())
    }

    @Test
    fun productValidationRequiresNonBlankName() {
        val valid = Product(EntityId("p1"), "Phone", null, null, null, ProductStatus.PUBLISHED, ProductType.SIMPLE, Pricing(null, null, false), null, emptyList(), emptyList(), emptyList(), null)
        assertTrue(valid.validate())
        assertFalse(valid.copy(name = "").validate())
    }

    @Test
    fun orderValidationRejectsNegativeQuantities() {
        val validItem = OrderItem(EntityId("item-1"), EntityId("product-1"), null, "Item", 1.0, "10", "10")
        val invalidItem = validItem.copy(quantity = -1.0)
        fun order(item: OrderItem) = Order(EntityId("order-1"), OrderStatus.PROCESSING, null, null, null, null, emptyList(), emptyList(), emptyList(), listOf(item), EntityTimestamp.parse("2026-01-01T00:00:00Z"))
        assertTrue(order(validItem).validate())
        assertFalse(order(invalidItem).validate())
    }

    @Test
    fun pendingOperationValidationRejectsNegativeRetryCount() {
        val valid = PendingOperation(EntityId("op-1"), StoreId("store-1"), "product", EntityId("product-1"), OperationType.UPDATE, "{}", "hash", 0, null)
        assertTrue(valid.validate())
        assertFalse(valid.copy(retryCount = -1).validate())
    }

    @Test
    fun productAndOrderModelsPreserveCurrencyMetadata() {
        val product = Product(EntityId("p1"), "Phone", "SKU-1", null, null, ProductStatus.PUBLISHED, ProductType.SIMPLE, Pricing("100", null, false), null, emptyList(), emptyList(), emptyList(), null, "EUR", "€", "right", ".", ",", 2)
        val order = Order(EntityId("o1"), OrderStatus.COMPLETED, null, null, null, null, emptyList(), emptyList(), emptyList(), emptyList(), null, total = "100", currency = "EUR")
        assertEquals("EUR", product.currency)
        assertEquals("€", product.currencySymbol)
        assertEquals("EUR", order.currency)
        assertEquals("100", order.total)
    }
}
