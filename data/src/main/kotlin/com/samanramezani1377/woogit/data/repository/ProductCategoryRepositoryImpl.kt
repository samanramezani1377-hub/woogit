package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.repository.ProductCategoryRepository
import com.samanramezani1377.woogit.data.network.HttpApiException
import com.samanramezani1377.woogit.data.network.WooCommerceClientProvider

class ProductCategoryRepositoryImpl(private val provider: WooCommerceClientProvider) : ProductCategoryRepository {
    override suspend fun list(storeId: StoreId, page: Int, perPage: Int, search: String?): CoreResult<List<IdName>> =
        provider.client(storeId).fold(
            { (store, api) -> api.productCategories(store.baseUrl, page, perPage, search).fold(
                onSuccess = { categories -> CoreResult.Success(categories.map { IdName(EntityId(it.id.toString()), it.name) }) },
                onFailure = { CoreResult.Failure(it.toDomain()) },
            ) },
            { CoreResult.Failure(it) },
        )

    private fun Throwable.toDomain(): DomainError = when (this) {
        is HttpApiException -> when (statusCode) {
            401 -> DomainError.Authentication("Authentication failed")
            403 -> DomainError.Permission("Category permission denied")
            404 -> DomainError.NotFound("product categories", statusCode.toString())
            429 -> DomainError.RateLimited("Rate limited")
            in 500..599 -> DomainError.Server("Category server error")
            else -> DomainError.Unknown("Category HTTP $statusCode")
        }
        else -> DomainError.Network(message ?: "Network failure")
    }
}
