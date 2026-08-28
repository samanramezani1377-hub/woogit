package com.samanramezani1377.woogit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class DashboardViewModelFactory(
    private val dependencies: V1PresentationDependencies,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }

        return DashboardViewModel(
            loadOrders = {
                when (val result = dependencies.getOrders(
                    dependencies.initialStoreId
                        ?.let(::com.samanramezani1377.woogit.core.domain.entity.StoreId)
                        ?: error("Store is not connected"),
                    1,
                    30,
                    null,
                    null,
                )) {
                    is com.samanramezani1377.woogit.core.domain.error.CoreResult.Success -> result.value
                    is com.samanramezani1377.woogit.core.domain.error.CoreResult.Failure -> {
                        error("Failed to load orders")
                    }
                }
            },
            loadProducts = {
                when (val result = dependencies.getProducts(
                    dependencies.initialStoreId
                        ?.let(::com.samanramezani1377.woogit.core.domain.entity.StoreId)
                        ?: error("Store is not connected"),
                    1,
                    30,
                    null,
                )) {
                    is com.samanramezani1377.woogit.core.domain.error.CoreResult.Success -> result.value
                    is com.samanramezani1377.woogit.core.domain.error.CoreResult.Failure -> {
                        error("Failed to load products")
                    }
                }
            },
        ) as T
    }
}
