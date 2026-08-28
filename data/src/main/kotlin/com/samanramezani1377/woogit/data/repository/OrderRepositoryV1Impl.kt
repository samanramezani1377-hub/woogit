package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.network.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

private val mutationJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private fun payloadHash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
@Serializable private data class OrderMutation(val status: String)

private fun OrderStatus.toWooValue(): String = when (this) {
    OrderStatus.PENDING -> "pending"
    OrderStatus.PROCESSING -> "processing"
    OrderStatus.ON_HOLD -> "on-hold"
    OrderStatus.COMPLETED -> "completed"
    OrderStatus.CANCELLED -> "cancelled"
    OrderStatus.REFUNDED -> "refunded"
    OrderStatus.FAILED -> "failed"
    OrderStatus.OTHER -> "pending"
}

private fun WooOrderTypedDto.toDomain(): Order {
    val modified = date_modified_gmt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    return Order(
        EntityId(id.toString()),
        when (status) {
            "pending" -> OrderStatus.PENDING
            "processing" -> OrderStatus.PROCESSING
            "on-hold" -> OrderStatus.ON_HOLD
            "completed" -> OrderStatus.COMPLETED
            "cancelled" -> OrderStatus.CANCELLED
            "refunded" -> OrderStatus.REFUNDED
            "failed" -> OrderStatus.FAILED
            else -> OrderStatus.OTHER
        },
        customer_id.takeIf { it != 0L }?.let { Customer(EntityId(it.toString()), listOfNotNull(billing?.first_name, billing?.last_name).joinToString(" "), billing?.phone) },
        billing?.let { Address(it.first_name, it.last_name, it.company, it.address_1, it.address_2, it.city, it.state, it.postcode, it.country, it.phone) },
        shipping?.let { Address(it.first_name, it.last_name, it.company, it.address_1, it.address_2, it.city, it.state, it.postcode, it.country, it.phone) },
        Payment(payment_method, payment_method_title, transaction_id, set_paid == true),
        shipping_lines.map { ShippingLine(it.method_id, it.method_title, it.total) },
        coupon_lines.map { Discount(it.code, it.discount) },
        emptyList(),
        line_items.map { OrderItem(EntityId(it.id.toString()), it.product_id.takeIf { v -> v != 0L }?.let { v -> EntityId(v.toString()) }, it.variation_id.takeIf { v -> v != 0L }?.let { v -> EntityId(v.toString()) }, it.name, it.quantity, it.subtotal, it.total) },
        modified,
        number.ifBlank { id.toString() },
        total,
        currency,
    )
}

class OrderRepositoryV1Impl(
    private val local: LocalOrderDataSource<Order>,
    private val provider: WooCommerceClientProvider,
    private val coordinator: MutationCoordinator,
    private val pending: PendingOperationRepository,
) : OrderRepository {
    override suspend fun get(storeId: StoreId, id: EntityId): CoreResult<Order> = provider.client(storeId).fold(
        { (store, api) -> api.order(store.baseUrl, id.value.toLong()).fold({ remote -> runCatching { remote.toDomain() }.fold({ value -> local.upsert(storeId, value); CoreResult.Success(value) }, { CoreResult.Failure(DomainError.Network("سفارش دریافتی از فروشگاه قابل پردازش نیست.")) }) }, { local.get(storeId, id) }) },
        { local.get(storeId, id) },
    )

    override suspend fun list(storeId: StoreId, page: Int, perPage: Int, search: String?, status: String?): CoreResult<List<Order>> {
        val cached = if (page == 1) local.list(storeId) else null
        return provider.client(storeId).fold(
            { (store, api) -> api.orders(store.baseUrl, page, perPage, search, status).fold({ values ->
                val mapped = values.mapNotNull { remote -> runCatching { remote.toDomain() }.getOrNull() }
                mapped.forEach { local.upsert(storeId, it) }
                if (values.isNotEmpty() && mapped.isEmpty()) CoreResult.Failure(DomainError.Network("سفارش‌های دریافتی از فروشگاه قابل پردازش نیستند.")) else CoreResult.Success(mapped)
            }, { cached ?: CoreResult.Failure(it.toDomain()) }) },
            { cached ?: CoreResult.Failure(it) },
        )
    }

    override suspend fun update(storeId: StoreId, id: EntityId, order: Order): CoreResult<Order> {
        val wooStatus = order.status.toWooValue()
        val payload = mutationJson.encodeToString(OrderMutation(wooStatus))
        val operation = PendingOperation(EntityId("order-update-${storeId.value}-${id.value}-${payloadHash(payload).take(16)}"), storeId, "order", id, OperationType.UPDATE, payload, payloadHash(payload), 0, null, null)
        val localResult = coordinator.execute(operation) { local.upsert(storeId, order) }
        if (localResult is CoreResult.Failure) return localResult
        return provider.client(storeId).fold(
            { (store, api) ->
                api.updateOrder(store.baseUrl, id.value.toLong(), WooOrderTypedDto(id.value.toLong(), number = order.number, status = wooStatus, total = order.total ?: "0", currency = order.currency ?: "", customer_id = order.customer?.id?.value?.toLongOrNull() ?: 0L)).fold(
                    { remote ->
                        val value = remote.toDomain()
                        local.upsert(storeId, value)
                        pending.markSucceeded(operation.id)
                        CoreResult.Success(value)
                    },
                    { error ->
                        if (error is HttpApiException && error.statusCode in 408..599) CoreResult.Success(order)
                        else CoreResult.Failure(error.toDomain())
                    },
                )
            },
            { error -> if (error.recoverable) CoreResult.Success(order) else CoreResult.Failure(error) },
        )
    }
}

private fun Throwable.toDomain(): DomainError = when (this) {
    is HttpApiException -> {
        val message = WordPressErrorMapper.message(statusCode, body)
        when (statusCode) {
            401 -> DomainError.Authentication(message)
            403 -> DomainError.Permission(message)
            404 -> DomainError.NotFound("remote", message)
            409 -> DomainError.Conflict(message)
            400, 405, 415, 422 -> DomainError.Validation(message)
            429 -> DomainError.RateLimited(message)
            in 500..599 -> DomainError.Server(message)
            else -> DomainError.Unknown(message)
        }
    }
    else -> DomainError.Network(message ?: "ارتباط با فروشگاه برقرار نشد. اتصال اینترنت و آدرس فروشگاه را بررسی کنید.")
}
