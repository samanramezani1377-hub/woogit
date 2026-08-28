package com.samanramezani1377.woogit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ConnectionState
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.presentation.PresentationErrorMapper
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal data class DashboardUiState(
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val ordersCount: String get() = DashboardStateMapper.ordersCount(orders)
    val productsCount: String get() = DashboardStateMapper.productsCount(products)
    val pendingCount: String get() = DashboardStateMapper.pendingCount(orders)
    val revenue: String get() = DashboardStateMapper.revenue(orders)
}

internal class DashboardViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var healthMonitorStarted = false

    fun refresh() {
        if (_uiState.value.loading) return
        viewModelScope.launch { refreshInternal(includeData = true) }
    }

    fun startConnectionHealthMonitor() {
        if (healthMonitorStarted) return
        healthMonitorStarted = true
        viewModelScope.launch {
            while (isActive) {
                checkConnection()
                delay(10_000L)
            }
        }
    }

    private suspend fun checkConnection() {
        val connection = when (val result = dependencies.getConnectionState(storeId)) {
            is CoreResult.Success -> result.value
            is CoreResult.Failure -> ConnectionState.ERROR
        }
        _uiState.value = _uiState.value.copy(connectionState = connection)
    }

    private suspend fun refreshInternal(includeData: Boolean) {
        _uiState.value = _uiState.value.copy(loading = includeData, error = null)
        val connection = when (val result = dependencies.getConnectionState(storeId)) {
            is CoreResult.Success -> result.value
            is CoreResult.Failure -> ConnectionState.ERROR
        }
        if (!includeData) {
            _uiState.value = _uiState.value.copy(connectionState = connection)
            return
        }
        val orders = when (val result = dependencies.getOrders(storeId, 1, 30, null, null)) {
            is CoreResult.Success -> result.value
            is CoreResult.Failure -> {
                _uiState.value = _uiState.value.copy(connectionState = connection, loading = false, error = PresentationErrorMapper.message(result.error))
                return
            }
        }
        val products = when (val result = dependencies.getProducts(storeId, 1, 30, null)) {
            is CoreResult.Success -> result.value
            is CoreResult.Failure -> {
                _uiState.value = _uiState.value.copy(connectionState = connection, loading = false, error = PresentationErrorMapper.message(result.error))
                return
            }
        }
        _uiState.value = DashboardUiState(orders = orders, products = products, connectionState = connection)
    }
}
