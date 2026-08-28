package com.samanramezani1377.woogit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DashboardUiState(
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val ordersCount: String
        get() = DashboardStateMapper.ordersCount(orders)

    val productsCount: String
        get() = DashboardStateMapper.productsCount(products)

    val pendingCount: String
        get() = DashboardStateMapper.pendingCount(orders)

    val revenue: String
        get() = DashboardStateMapper.revenue(orders)
}

/**
 * Presentation-only dashboard state holder.
 * Data loading is injected so the screen is independent from networking.
 */
internal class DashboardViewModel(
    private val loadOrders: suspend () -> List<Order>,
    private val loadProducts: suspend () -> List<Product>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        if (_uiState.value.loading) return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            runCatching {
                loadOrders() to loadProducts()
            }.onSuccess { (orders, products) ->
                _uiState.value = DashboardUiState(
                    orders = orders,
                    products = products,
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = throwable.message ?: "خطا در دریافت اطلاعات داشبورد",
                    )
                }
            }
        }
    }
}
