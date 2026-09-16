package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.network.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.security.MessageDigest
import java.util.UUID

private val mutationJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private fun payloadHash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun newOperationId(prefix: String) = "$prefix-${UUID.randomUUID()}"
private fun parseWooInstant(value: String?): Instant? { val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null; return runCatching { Instant.parse(raw) }.getOrNull() ?: runCatching { Instant.parse("${raw}Z") }.getOrNull() }
private fun OrderStatus.toWooValue(): String = when (this) { OrderStatus.PENDING -> "pending"; OrderStatus.PROCESSING -> "processing"; OrderStatus.ON_HOLD -> "on-hold"; OrderStatus.COMPLETED -> "completed"; OrderStatus.CANCELLED -> "cancelled"; OrderStatus.REFUNDED -> "refunded"; OrderStatus.FAILED -> "failed"; OrderStatus.OTHER -> "pending" }
private fun WooOrderNoteDto.toDomain(): OrderNote = OrderNote(id.takeIf { it > 0L }?.let { EntityId(it.toString()) }, note, customer_note)
private fun WooOrderTypedDto.toDomain(): Order { return Order(EntityId(id.toString()), when (status) { "pending" -> OrderStatus.PENDING; "processing" -> OrderStatus.PROCESSING; "on-hold" -> OrderStatus.ON_HOLD; "completed" -> OrderStatus.COMPLETED; "cancelled" -> OrderStatus.CANCELLED; "refunded" -> OrderStatus.REFUNDED; "failed" -> OrderStatus.FAILED; else -> OrderStatus.OTHER }, customer_id.takeIf { it != 0L }?.let { Customer(EntityId(it.toString()), listOfNotNull(billing?.first_name, billing?.last_name).joinToString(" "), billing?.email, billing = billing?.let { a -> Address(a.first_name,a.last_name,a.company,a.address_1,a.address_2,a.city,a.state,a.postcode,a.country,a.phone) }, shipping = shipping?.let { a -> Address(a.first_name,a.last_name,a.company,a.address_1,a.address_2,a.city,a.state,a.postcode,a.country,a.phone) }) }, billing?.let { Address(it.first_name,it.last_name,it.company,it.address_1,it.address_2,it.city,it.state,it.postcode,it.country,it.phone) }, shipping?.let { Address(it.first_name,it.last_name,it.company,it.address_1,it.address_2,it.city,it.state,it.postcode,it.country,it.phone) }, Payment(payment_method,payment_method_title,transaction_id,date_paid_gmt?.isNotBlank()==true), shipping_lines.map { ShippingLine(it.method_id,it.method_title,it.total) }, coupon_lines.map { Discount(it.code,it.discount) }, emptyList(), line_items.map { OrderItem(EntityId(it.id.toString()),it.product_id.takeIf{v->v!=0L}?.let{v->EntityId(v.toString())},it.variation_id.takeIf{v->v!=0L}?.let{v->EntityId(v.toString())},it.name,it.quantity,it.subtotal,it.total) }, parseWooInstant(date_modified_gmt), number.ifBlank{id.toString()}, total, currency, createdAt = parseWooInstant(date_created_gmt)) }
private fun formatWooMoney(value:String,settings:WooSystemStatusSettingsDto):String{val amount=value.toBigDecimalOrNull()?:BigDecimal.ZERO;val symbols=DecimalFormatSymbols().apply{groupingSeparator=settings.thousand_separator.firstOrNull()?:',';decimalSeparator=settings.decimal_separator.firstOrNull()?:'.'};val decimals=settings.number_of_decimals.coerceAtLeast(0);val pattern=if(decimals==0)"#,##0" else "#,##0."+"0".repeat(decimals);val formatted=DecimalFormat(pattern,symbols).format(amount);val symbol=settings.currency_symbol.ifBlank{settings.currency};return when(settings.currency_position){"right"->"$formatted$symbol";"left_space"->"$symbol $formatted";"right_space"->"$formatted $symbol";else->"$symbol$formatted"}}

private fun Address.toChangedJson(previous: Address?, email: String? = null): JsonObject = JsonObject(buildMap {
    if (previous == null || firstName != previous.firstName) put("first_name", JsonPrimitive(firstName ?: ""))
    if (previous == null || lastName != previous.lastName) put("last_name", JsonPrimitive(lastName ?: ""))
    if (previous == null || company != previous.company) put("company", JsonPrimitive(company ?: ""))
    if (previous == null || address1 != previous.address1) put("address_1", JsonPrimitive(address1 ?: ""))
    if (previous == null || address2 != previous.address2) put("address_2", JsonPrimitive(address2 ?: ""))
    if (previous == null || city != previous.city) put("city", JsonPrimitive(city ?: ""))
    if (previous == null || state != previous.state) put("state", JsonPrimitive(state ?: ""))
    if (previous == null || postcode != previous.postcode) put("postcode", JsonPrimitive(postcode ?: ""))
    if (previous == null || country != previous.country) put("country", JsonPrimitive(country ?: ""))
    if (previous == null || phone != previous.phone) put("phone", JsonPrimitive(phone ?: ""))
    email?.let { put("email", JsonPrimitive(it)) }
})

private fun Order.toUpdateJson(previous: Order?): JsonObject {
    val fields = linkedMapOf<String, kotlinx.serialization.json.JsonElement>()
    if (previous == null || status != previous.status) fields["status"] = JsonPrimitive(status.toWooValue())
    if (previous != null && customer?.id != previous.customer?.id) {
        customer?.id?.value?.toLongOrNull()?.takeIf { it > 0L }?.let { fields["customer_id"] = JsonPrimitive(it) }
    }
    if (billing != previous?.billing || customer?.email != previous?.customer?.email) fields["billing"] = billing?.toChangedJson(previous?.billing, customer?.email) ?: JsonObject(emptyMap())
    if (shipping != previous?.shipping) fields["shipping"] = shipping?.toChangedJson(previous?.shipping) ?: JsonObject(emptyMap())
    if (previous == null || payment != previous.payment) {
        payment?.methodId?.let { fields["payment_method"] = JsonPrimitive(it) }
        payment?.methodTitle?.let { fields["payment_method_title"] = JsonPrimitive(it) }
        payment?.transactionId?.let { fields["transaction_id"] = JsonPrimitive(it) }
        fields["set_paid"] = JsonPrimitive(payment?.paid == true)
    }
    if (previous == null || items != previous.items) fields["line_items"] = JsonArray(items.map { item -> JsonObject(buildMap { put("id", JsonPrimitive(item.id.value.toLongOrNull() ?: 0L)); put("quantity", JsonPrimitive(item.quantity)) }) })
    if (previous == null || shippingLines != previous.shippingLines) fields["shipping_lines"] = JsonArray(shippingLines.map { line -> JsonObject(buildMap { put("method_id", JsonPrimitive(line.methodId ?: "")); put("method_title", JsonPrimitive(line.methodTitle ?: "")); put("total", JsonPrimitive(line.total ?: "0")) }) })
    return JsonObject(fields)
}

class OrderRepositoryV1Impl(private val local:LocalOrderDataSource<Order>,private val provider:WooCommerceClientProvider,private val coordinator:MutationCoordinator,private val pending:PendingOperationRepository,private val refreshScope:CoroutineScope):OrderRepository {
 override suspend fun get(storeId:StoreId,id:EntityId):CoreResult<Order>{val cached=local.get(storeId,id);if(cached is CoreResult.Success){refreshScope.launch{refreshDetail(storeId,id)};return cached};return refreshDetail(storeId,id)}
 private suspend fun refreshDetail(storeId:StoreId,id:EntityId): CoreResult<Order> = provider.client(storeId).fold({(store,api)->api.order(store.baseUrl,id.value.toLong()).fold({remote->val base=remote.toDomain();val cachedNotes=when(val cached=local.get(storeId,id)){is CoreResult.Success->cached.value.notes;is CoreResult.Failure->emptyList()};val notes=api.orderNotes(store.baseUrl,id.value.toLong(),1,100).getOrNull() ?: cachedNotes;val value=base.copy(notes=notes);local.upsert(storeId,value);CoreResult.Success(value)},{error->CoreResult.Failure(error.toDomain())})},{error->local.get(storeId,id)})
 override suspend fun list(storeId:StoreId,page:Int,perPage:Int,search:String?,status:String?):CoreResult<List<Order>>{val isDefaultList=page==1&&search.isNullOrBlank()&&status.isNullOrBlank();if(isDefaultList){val cached=when(val result=local.list(storeId)){is CoreResult.Success->result.value;is CoreResult.Failure->emptyList()};if(cached.isNotEmpty()){refreshScope.launch{refreshFirstPage(storeId,perPage)};return CoreResult.Success(cached.take(perPage))}};return fetchPage(storeId,page,perPage,search,status)}
 override suspend fun count(storeId:StoreId,search:String?,status:String?):CoreResult<Int> = provider.client(storeId).fold({(store,api)->api.ordersTotal(store.baseUrl,search?.trim()?.takeIf{it.isNotEmpty()},status?.trim()?.takeIf{it.isNotEmpty()}).fold({CoreResult.Success(it)},{CoreResult.Failure(it.toDomain())})},{CoreResult.Failure(it)})
 private suspend fun refreshFirstPage(storeId:StoreId,perPage:Int){fetchPage(storeId,1,perPage,null,null)}
 suspend fun refresh(storeId:StoreId,page:Int,perPage:Int):CoreResult<List<Order>> = fetchPage(storeId,page,perPage,null,null)
 private suspend fun fetchPage(storeId:StoreId,page:Int,perPage:Int,search:String?,status:String?):CoreResult<List<Order>> = provider.client(storeId).fold({(store,api)->api.orders(store.baseUrl,page,perPage,search,status).fold({values->val mapped=values.mapNotNull{remote->runCatching{remote.toDomain()}.getOrNull()};mapped.forEach{local.upsert(storeId,it)};val ordered=mapped.sortedWith(compareByDescending<Order>{it.createdAt?.toString() ?: ""}.thenByDescending{it.id.value.toLongOrNull()?:Long.MIN_VALUE});if(values.isNotEmpty()&&mapped.isEmpty())CoreResult.Failure(DomainError.Network("سفارش‌های دریافتی از فروشگاه قابل پردازش نیستند.")) else CoreResult.Success(ordered)},{error->val cached=if(page==1&&search.isNullOrBlank()&&status.isNullOrBlank())local.list(storeId) else null;cached?:CoreResult.Failure(error.toDomain())})},{error->val cached=if(page==1&&search.isNullOrBlank()&&status.isNullOrBlank())local.list(storeId) else null;cached?:CoreResult.Failure(error)})
 override suspend fun salesSummary(storeId:StoreId):CoreResult<SalesSummary> = provider.client(storeId).fold({(store,api)->val settingsResult=api.validate(store.baseUrl);val reportResult=api.salesReport(store.baseUrl,"2000-01-01",LocalDate.now().toString());if(settingsResult.isFailure||reportResult.isFailure){val error=settingsResult.exceptionOrNull()?:reportResult.exceptionOrNull()?:Exception("WooCommerce sales report failed");return@fold CoreResult.Failure(error.toDomain())};val settings=settingsResult.getOrThrow().settings;val report=reportResult.getOrThrow();CoreResult.Success(SalesSummary(report.net_sales,settings.currency,settings.currency_symbol,settings.currency_position,settings.thousand_separator,settings.decimal_separator,settings.number_of_decimals))},{CoreResult.Failure(it)})
 override suspend fun update(storeId:StoreId,id:EntityId,order:Order):CoreResult<Order>{val previous=when(val result=local.get(storeId,id)){is CoreResult.Success->result.value;is CoreResult.Failure->null};val updateJson=order.toUpdateJson(previous);if(updateJson.isEmpty())return CoreResult.Success(order);val payload=updateJson.toString();val hash=payloadHash(payload);val operation=PendingOperation(EntityId(newOperationId("order-update")),storeId,"order",id,OperationType.UPDATE,payload,hash,0,null,null);val localResult=coordinator.execute(operation){local.upsert(storeId,order)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->api.updateOrderFields(store.baseUrl,id.value.toLong(),updateJson,operation.id.value).fold({remote->val value=remote.toDomain();local.upsert(storeId,value);pending.markSucceeded(operation.id);CoreResult.Success(value)},{error->if(error is HttpApiException&&error.statusCode in 408..599)CoreResult.Success(order)else CoreResult.Failure(error.toDomain())})},{error->if(error.recoverable)CoreResult.Success(order)else CoreResult.Failure(error)})}
}
private fun Throwable.toDomain():DomainError=when(this){is HttpApiException->{val message=WordPressErrorMapper.message(statusCode,body);when(statusCode){401->DomainError.Authentication(message);403->DomainError.Permission(message);404->DomainError.NotFound("remote",message);409->DomainError.Conflict(message);400,405,415,422->DomainError.Validation(message);429->DomainError.RateLimited(message);in 500..599->DomainError.Server(message);else->DomainError.Unknown(message)}}else->DomainError.Network(message?:"ارتباط با فروشگاه برقرار نشد. اتصال اینترنت و آدرس فروشگاه را بررسی کنید.")}
