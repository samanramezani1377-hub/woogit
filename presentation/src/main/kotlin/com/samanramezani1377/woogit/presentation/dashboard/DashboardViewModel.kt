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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class DashboardUiState(
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val loading: Boolean = false,
    val error: String? = null,
    val lastConnectionCheckAtMillis: Long? = null,
)

internal class DashboardViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var healthMonitorStarted = false
    private var healthCheckInFlight = false

    fun refresh() {
        if (_uiState.value.loading) return
        viewModelScope.launch { refreshInternal() }
    }

    fun startConnectionHealthMonitor() {
        if (healthMonitorStarted) return
        healthMonitorStarted = true
        viewModelScope.launch {
            var delayMillis = 10_000L
            while (isActive) {
                val result = checkConnection()
                delayMillis = if (result == ConnectionState.CONNECTED) 10_000L else (delayMillis * 2).coerceAtMost(60_000L)
                delay(delayMillis)
            }
        }
    }

    fun onNetworkAvailable() {
        viewModelScope.launch { checkConnection() }
    }

    private suspend fun checkConnection(): ConnectionState {
        if (healthCheckInFlight) return _uiState.value.connectionState
        healthCheckInFlight = true
        return try {
            val connection = withTimeoutOrNull(5_000L) {
                when (val result = dependencies.getConnectionState(storeId)) {
                    is CoreResult.Success -> result.value
                    is CoreResult.Failure -> ConnectionState.ERROR
                }
            } ?: ConnectionState.ERROR
            _uiState.value = _uiState.value.copy(
                connectionState = connection,
                lastConnectionCheckAtMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            )
            connection
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            _uiState.value = _uiState.value.copy(
                connectionState = ConnectionState.ERROR,
                lastConnectionCheckAtMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            )
            ConnectionState.ERROR
        } finally {
            healthCheckInFlight = false
        }
    }

    private suspend fun refreshInternal() {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        val connection = checkConnection()
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
        _uiState.value = _uiState.value.copy(orders = orders, products = products, connectionState = connection, loading = false)
    }
}
