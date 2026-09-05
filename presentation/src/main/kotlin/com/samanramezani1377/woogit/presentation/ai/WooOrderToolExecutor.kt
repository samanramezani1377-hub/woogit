package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Address
import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Discount
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderItem
import com.samanramezani1377.woogit.core.domain.model.OrderNote
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Payment
import com.samanramezani1377.woogit.core.domain.model.ShippingLine
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

internal class WooOrderToolExecutor(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) {
    suspend fun execute(name: String, arguments: JSONObject): String = when (name) {
        "orders_list" -> list(arguments)
        "orders_get" -> get(arguments)
        "orders_update_status" -> updateStatus(arguments)
        else -> failure("ابزار سفارش ناشناخته است: $name")
    }

    private suspend fun list(a: JSONObject): String = coroutineScope {
        val page = a.optInt("page", 1).coerceAtLeast(1)
        val perPage = a.optInt("perPage", 20).coerceIn(1, 99)
        val search = a.optString("search").takeIf { it.isNotBlank() }
        val status = a.optString("status").takeIf { it.isNotBlank() }
        val listDeferred = async {
            dependencies.getOrders(storeId, page, (perPage + 1).coerceAtMost(100), search, status)
        }
        val countDeferred = async { dependencies.getOrders.count(storeId, search, status) }
        val result = listDeferred.await()
        val totalResult = countDeferred.await()
        when (result) {
            is CoreResult.Failure -> failure(result.error.toString())
            is CoreResult.Success -> {
                val orders = result.value as? List<*> ?: emptyList<Any?>()
                val visible = orders.take(perPage).mapNotNull { it as? Order }
                val probeHasMore = orders.size > perPage
                val total = (totalResult as? CoreResult.Success)?.value
                val totalPages = total?.let { if (it == 0) 0 else (it + perPage - 1) / perPage }
                val hasMore = total?.let { page < totalPages.orZero() } ?: probeHasMore
                JSONObject()
                    .put("ok", true)
                    .put("type", "orders")
                    .put("description", "این داده‌ها لیست سفارش‌های فروشگاه هستند.")
                    .put("page", page)
                    .put("perPage", perPage)
                    .put("count", visible.size)
                    .put("totalCount", total ?: JSONObject.NULL)
                    .put("totalPages", totalPages ?: JSONObject.NULL)
                    .put("hasMore", hasMore)
                    .put("lastPage", totalPages ?: if (hasMore) JSONObject.NULL else page)
                    .put("endOfCollection", !hasMore)
                    .put("data", JSONArray().apply { visible.forEach { put(orderJson(it)) } })
                    .toString()
            }
        }
    }

    private suspend fun get(a: JSONObject): String = when (
        val result = dependencies.getOrder(storeId, EntityId(a.getLong("id").toString()))
    ) {
        is CoreResult.Failure -> failure(result.error.toString())
        is CoreResult.Success -> {
            val order = result.value as? Order ?: return failure("اطلاعات سفارش قابل دریافت نیست.")
            JSONObject()
                .put("ok", true)
                .put("type", "order")
                .put("description", "این داده اطلاعات کامل سفارش است.")
                .put("data", orderJson(order))
                .toString()
        }
    }

    private suspend fun updateStatus(a: JSONObject): String {
        val id = EntityId(a.getLong("id").toString())
        val requested = runCatching { OrderStatus.valueOf(a.getString("status").trim().uppercase()) }
            .getOrElse { return failure("status سفارش نامعتبر است.") }
        return when (val current = dependencies.getOrder(storeId, id)) {
            is CoreResult.Failure -> failure(current.error.toString())
            is CoreResult.Success -> when (val result = dependencies.updateOrder(storeId, id, current.value.copy(status = requested))) {
                is CoreResult.Failure -> failure(result.error.toString())
                is CoreResult.Success -> when (val reread = dependencies.getOrder(storeId, id)) {
                    is CoreResult.Failure -> failure("تغییر وضعیت سفارش ارسال شد، اما وضعیت نهایی قابل تأیید نیست: ${reread.error}")
                    is CoreResult.Success -> if (reread.value.status == requested) JSONObject()
                        .put("ok", true).put("verified", true).put("operation", "orders_update_status")
                        .put("data", JSONObject().put("orderId", id.value).put("status", requested.name).put("statusLabel", statusLabel(requested))).toString()
                    else failure("تغییر وضعیت سفارش تأیید نشد؛ وضعیت نهایی ${reread.value.status.name} است.")
                }
            }
        }
    }

    private fun orderJson(order: Order): JSONObject = JSONObject()
        .put("id", order.id.value).put("number", order.number).put("status", order.status.name)
        .put("statusLabel", statusLabel(order.status)).put("total", order.total ?: JSONObject.NULL)
        .put("currency", order.currency ?: JSONObject.NULL).put("customer", order.customer?.let(::customerJson) ?: JSONObject.NULL)
        .put("billing", order.billing?.let(::addressJson) ?: JSONObject.NULL).put("shipping", order.shipping?.let(::addressJson) ?: JSONObject.NULL)
        .put("payment", order.payment?.let(::paymentJson) ?: JSONObject.NULL)
        .put("shippingLines", JSONArray().apply { order.shippingLines.forEach { put(shippingJson(it)) } })
        .put("discounts", JSONArray().apply { order.discounts.forEach { put(discountJson(it)) } })
        .put("notes", JSONArray().apply { order.notes.forEach { put(noteJson(it)) } })
        .put("items", JSONArray().apply { order.items.forEach { put(itemJson(it)) } })
        .put("modifiedAt", order.modifiedAt ?: JSONObject.NULL)

    private fun customerJson(value: Customer) = JSONObject().put("id", value.id?.value ?: JSONObject.NULL).put("name", value.name).put("email", value.email ?: JSONObject.NULL)
    private fun addressJson(value: Address) = JSONObject().put("firstName", value.firstName ?: JSONObject.NULL).put("lastName", value.lastName ?: JSONObject.NULL).put("company", value.company ?: JSONObject.NULL).put("address1", value.address1 ?: JSONObject.NULL).put("address2", value.address2 ?: JSONObject.NULL).put("city", value.city ?: JSONObject.NULL).put("state", value.state ?: JSONObject.NULL).put("postcode", value.postcode ?: JSONObject.NULL).put("country", value.country ?: JSONObject.NULL).put("phone", value.phone ?: JSONObject.NULL)
    private fun paymentJson(value: Payment) = JSONObject().put("methodId", value.methodId ?: JSONObject.NULL).put("methodTitle", value.methodTitle ?: JSONObject.NULL).put("transactionId", value.transactionId ?: JSONObject.NULL).put("paid", value.paid)
    private fun shippingJson(value: ShippingLine) = JSONObject().put("methodId", value.methodId ?: JSONObject.NULL).put("methodTitle", value.methodTitle ?: JSONObject.NULL).put("total", value.total ?: JSONObject.NULL)
    private fun discountJson(value: Discount) = JSONObject().put("code", value.code).put("total", value.total)
    private fun noteJson(value: OrderNote) = JSONObject().put("id", value.id?.value ?: JSONObject.NULL).put("content", value.content).put("customerNote", value.customerNote)
    private fun itemJson(value: OrderItem) = JSONObject().put("id", value.id.value).put("productId", value.productId?.value ?: JSONObject.NULL).put("variationId", value.variationId?.value ?: JSONObject.NULL).put("name", value.name).put("quantity", value.quantity).put("subtotal", value.subtotal).put("total", value.total)

    private fun statusLabel(status: OrderStatus): String = when (status) {
        OrderStatus.PENDING -> "در انتظار پرداخت"
        OrderStatus.PROCESSING -> "در حال پردازش"
        OrderStatus.ON_HOLD -> "در انتظار"
        OrderStatus.COMPLETED -> "تکمیل شده"
        OrderStatus.CANCELLED -> "لغو شده"
        OrderStatus.REFUNDED -> "مسترد شده"
        OrderStatus.FAILED -> "ناموفق"
        OrderStatus.OTHER -> "سایر"
    }

    private fun Int?.orZero(): Int = this ?: 0
    private fun failure(message: String) = JSONObject().put("ok", false).put("verified", false).put("error", message).toString()
}
