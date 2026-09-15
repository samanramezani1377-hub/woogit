package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.Address
import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.repository.CustomerRepository
import com.samanramezani1377.woogit.core.domain.repository.LocalCustomerDataSource
import com.samanramezani1377.woogit.data.network.HttpApiException
import com.samanramezani1377.woogit.data.network.WooAddressDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceWriteDto
import com.samanramezani1377.woogit.data.network.WooCommerceClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

private fun WooAddressDto.toDomain() = Address(first_name,last_name,company,address_1,address_2,city,state,postcode,country,phone)
private fun Address.toWoo() = WooAddressDto(firstName,lastName,company,address1,address2,city,state,postcode,country,phone)
private fun WooCustomerCommerceDto.toDomain() = Customer(
    id = EntityId(id.toString()),
    name = listOfNotNull(first_name?.takeIf { it.isNotBlank() },last_name?.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { username ?: email ?: "مشتری #$id" },
    email = email,
    username = username,
    role = role,
    phone = billing?.phone ?: shipping?.phone,
    billing = billing?.toDomain(),
    shipping = shipping?.toDomain(),
    ordersCount = orders_count,
    totalSpent = total_spent,
    dateCreated = date_created?.let { runCatching { Instant.parse(it) }.getOrNull() },
    dateModified = date_modified?.let { runCatching { Instant.parse(it) }.getOrNull() },
)

private fun Customer.toWriteDto() = WooCustomerCommerceWriteDto(
    email = email,
    username = username,
    first_name = billing?.firstName ?: name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() },
    last_name = billing?.lastName ?: name.split(" ").drop(1).joinToString(" ").takeIf { it.isNotBlank() },
    role = role,
    billing = billing?.toWoo() ?: phone?.let { WooAddressDto(phone = it) },
    shipping = shipping?.toWoo(),
)

class CustomerRepositoryV1Impl(
    private val local: LocalCustomerDataSource<Customer>,
    private val provider: WooCommerceClientProvider,
    private val refreshScope: CoroutineScope,
) : CustomerRepository {
    override suspend fun get(storeId: StoreId, id: EntityId): CoreResult<Customer> {
        val cached = local.get(storeId,id)
        if (cached is CoreResult.Success) {
            refreshScope.launch { refreshDetail(storeId,id) }
            return cached
        }
        return refreshDetail(storeId,id)
    }

    override suspend fun create(storeId: StoreId, customer: Customer): CoreResult<Customer> =
        when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val api = client.value.second
                api.decodeCustomer(api.createCustomer(customer.toWriteDto(), "customer-create-${System.currentTimeMillis()}"))
                    .fold(
                        onSuccess = { value -> value.toDomain().also { local.upsert(storeId,it) }.let { CoreResult.Success(it) } },
                        onFailure = { CoreResult.Failure(it.toDomain()) },
                    )
            }
            is CoreResult.Failure -> CoreResult.Failure(client.error)
        }

    override suspend fun update(storeId: StoreId, id: EntityId, customer: Customer): CoreResult<Customer> {
        val numericId = id.value.toLongOrNull()
            ?: return CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است."))
        return when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val api = client.value.second
                api.decodeCustomer(api.updateCustomer(numericId, customer.toWriteDto(), "customer-update-$numericId-${System.currentTimeMillis()}"))
                    .fold(
                        onSuccess = { value -> value.toDomain().also { local.upsert(storeId,it) }.let { CoreResult.Success(it) } },
                        onFailure = { CoreResult.Failure(it.toDomain()) },
                    )
            }
            is CoreResult.Failure -> CoreResult.Failure(client.error)
        }
    }

    override suspend fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit> {
        val numericId = id.value.toLongOrNull()
            ?: return CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است."))
        return when (val client = provider.commerceClient(storeId)) {
            is CoreResult.Success -> {
                val response = client.value.second.deleteCustomer(numericId, true, "customer-delete-$numericId-${System.currentTimeMillis()}")
                if (response.statusCode in 200..299) {
                    local.delete(storeId,id)
                    CoreResult.Success(Unit)
                } else {
                    CoreResult.Failure(Throwable.toDomain(HttpApiException(response.statusCode, response.body)))
                }
            }
            is CoreResult.Failure -> CoreResult.Failure(client.error)
        }
    }

    private suspend fun refreshDetail(storeId: StoreId,id: EntityId): CoreResult<Customer> = when (val client = provider.commerceClient(storeId)) {
        is CoreResult.Success -> {
            val api = client.value.second
            val numericId = id.value.toLongOrNull() ?: return CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است."))
            api.decodeCustomer(api.getCustomer(numericId)).fold(
                onSuccess = { value -> value.toDomain().also { local.upsert(storeId,it) }.let { CoreResult.Success(it) } },
                onFailure = { error -> local.get(storeId,id).let { cached -> if (cached is CoreResult.Success) cached else CoreResult.Failure(error.toDomain()) } },
            )
        }
        is CoreResult.Failure -> local.get(storeId,id)
    }

    override suspend fun list(storeId: StoreId,page: Int,perPage: Int,search: String?): CoreResult<List<Customer>> {
        val normalized = search?.trim()?.takeIf { it.isNotEmpty() }
        val cached = local.list(storeId)
        if (page == 1 && cached is CoreResult.Success) {
            val filtered = cached.value.filter { customer ->
                normalized == null || listOf(customer.name,customer.email,customer.username,customer.phone)
                    .filterNotNull().any { it.contains(normalized, ignoreCase = true) }
            }
            if (filtered.isNotEmpty()) {
                refreshScope.launch { refresh(storeId,1,perPage,normalized) }
                return CoreResult.Success(filtered.take(perPage))
            }
        }
        return refresh(storeId,page,perPage,normalized)
    }

    override suspend fun refresh(storeId: StoreId,page: Int,perPage: Int,search: String?): CoreResult<List<Customer>> = when (val client = provider.commerceClient(storeId)) {
        is CoreResult.Success -> {
            val api = client.value.second
            api.decodeCustomers(api.listCustomers(page,perPage,search)).fold(
                onSuccess = { values ->
                    val mapped = values.map { it.toDomain() }
                    mapped.forEach { local.upsert(storeId,it) }
                    CoreResult.Success(mapped)
                },
                onFailure = { CoreResult.Failure(it.toDomain()) },
            )
        }
        is CoreResult.Failure -> {
            if (page == 1) local.list(storeId) else CoreResult.Failure(DomainError.Network("ارتباط با فروشگاه برقرار نشد."))
        }
    }
}

private fun Throwable.toDomain(): DomainError = when (this) {
    is HttpApiException -> when (statusCode) {
        401 -> DomainError.Authentication("احراز هویت مدیریت مشتریان ناموفق بود.")
        403 -> DomainError.Permission("دسترسی مدیریت مشتریان مجاز نیست.")
        404 -> DomainError.NotFound("customer", "مشتری پیدا نشد.")
        409 -> DomainError.Conflict("تعارض در اطلاعات مشتری.")
        400,405,415,422 -> DomainError.Validation("اطلاعات مشتری نامعتبر است.")
        429 -> DomainError.RateLimited("درخواست‌های مشتریان بیش از حد مجاز است.")
        in 500..599 -> DomainError.Server("سرور فروشگاه در مدیریت مشتری خطا داد.")
        else -> DomainError.Unknown("عملیات مشتری ناموفق بود.")
    }
    else -> DomainError.Network("ارتباط با فروشگاه برقرار نشد.")
}
