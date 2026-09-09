package com.samanramezani1377.woogit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.ConnectionState
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.SalesSummary
import com.samanramezani1377.woogit.presentation.PresentationErrorMapper
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.debug.DashboardSalesDebugSnapshot
import com.samanramezani1377.woogit.presentation.debug.PresentationTechnicalErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal data class DashboardUiState(
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val salesSummary: SalesSummary? = null,
    val ordersTotal: Int? = null,
    val processingTotal: Int? = null,
    val productsTotal: Int? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val loading: Boolean = false,
    val error: String? = null,
    val lastConnectionCheckAtMillis: Long? = null,
) {
    val ordersCount: String get() = DashboardStateMapper.ordersCount(ordersTotal ?: orders.size)
    val productsCount: String get() = DashboardStateMapper.productsCount(productsTotal ?: products.size)
    val processingCount: String get() = DashboardStateMapper.processingCount(processingTotal ?: orders.count { it.status.name == "PROCESSING" })
    val revenue: String get() = DashboardStateMapper.revenue(salesSummary)
}

internal class DashboardViewModel(private val dependencies: V1PresentationDependencies, private val storeId: StoreId) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val refreshMutex = Mutex()
    private var healthMonitorJob: Job? = null
    private var healthCheckInFlight = false

    fun refresh() {
        viewModelScope.launch {
            refreshMutex.withLock {
                if (_uiState.value.loading) return@withLock
                refreshInternal()
            }
        }
    }

    fun startConnectionHealthMonitor() {
        if (healthMonitorJob?.isActive == true) return
        healthMonitorJob = viewModelScope.launch {
            var interval = 10_000L
            while (isActive) {
                val state = checkConnection()
                interval = if (state == ConnectionState.CONNECTED) 10_000L else (interval * 2).coerceAtMost(60_000L)
                delay(interval)
            }
        }
    }

    fun stopConnectionHealthMonitor() { healthMonitorJob?.cancel(); healthMonitorJob = null }
    fun onNetworkAvailable() { viewModelScope.launch { checkConnection() } }
    override fun onCleared() { healthMonitorJob?.cancel(); super.onCleared() }

    private suspend fun checkConnection(): ConnectionState {
        if (healthCheckInFlight) return _uiState.value.connectionState
        healthCheckInFlight = true
        return try {
            val state = withTimeoutOrNull(5_000L) {
                when (val result = dependencies.getConnectionState(storeId)) {
                    is CoreResult.Success -> result.value
                    is CoreResult.Failure -> {
                        PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.checkConnection", "Connection check", PresentationErrorMapper.message(result.error), result.error.toString())
                        ConnectionState.ERROR
                    }
                }
            } ?: ConnectionState.ERROR.also {
                PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.checkConnection", "Connection timeout", "وضعیت اتصال قابل بررسی نبود.", "Connection check timed out after 5000ms")
            }
            _uiState.value = _uiState.value.copy(connectionState = state, lastConnectionCheckAtMillis = System.currentTimeMillis())
            state
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.checkConnection", "Connection check", "ارتباط با فروشگاه برقرار نشد.", throwable = e)
            _uiState.value = _uiState.value.copy(connectionState = ConnectionState.ERROR, lastConnectionCheckAtMillis = System.currentTimeMillis())
            ConnectionState.ERROR
        } finally {
            healthCheckInFlight = false
        }
    }

    private suspend fun loadLatestOrders(): CoreResult<List<Order>> =
        dependencies.getOrders(storeId, 1, 30, null, null)

    private suspend fun loadLatestProducts(): CoreResult<List<Product>> =
        dependencies.getProducts(storeId, 1, 30, null)

    private suspend fun refreshInternal() {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            val connection = checkConnection()

            val ordersResult = loadLatestOrders()
            val orders = when (ordersResult) {
                is CoreResult.Success -> ordersResult.value
                is CoreResult.Failure -> {
                    val message = PresentationErrorMapper.message(ordersResult.error)
                    PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Load orders", message, ordersResult.error.toString())
                    _uiState.value = _uiState.value.copy(connectionState = connection, loading = false, error = message)
                    return
                }
            }

            val ordersTotal = when (val result = dependencies.getOrders.count(storeId, null, null)) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> null
            }
            val processingTotal = when (val result = dependencies.getOrders.count(storeId, null, "processing")) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> null
            }

            val salesSummary = when (val result = dependencies.getSalesSummary(storeId)) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> {
                    PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Load sales summary", PresentationErrorMapper.message(result.error), result.error.toString())
                    _uiState.value.salesSummary
                }
            }

            val productsResult = loadLatestProducts()
            val products = when (productsResult) {
                is CoreResult.Success -> productsResult.value
                is CoreResult.Failure -> {
                    val message = PresentationErrorMapper.message(productsResult.error)
                    PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Load products", message, productsResult.error.toString())
                    _uiState.value = _uiState.value.copy(connectionState = connection, loading = false, error = message)
                    return
                }
            }
            val productsTotal = when (val result = dependencies.getProducts.count(storeId, null)) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> null
            }

            val newState = _uiState.value.copy(
                orders = orders,
                products = products,
                salesSummary = salesSummary,
                ordersTotal = ordersTotal,
                processingTotal = processingTotal,
                productsTotal = productsTotal,
                connectionState = connection,
                loading = false,
                error = null,
            )
            _uiState.value = newState
            DashboardSalesDebugSnapshot.update(orders, salesSummary, newState.revenue)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Unexpected dashboard refresh failure", "بارگذاری داشبورد با خطا مواجه شد.", e.toString())
            _uiState.value = _uiState.value.copy(loading = false, error = "بارگذاری داشبورد با خطا مواجه شد.")
        }
    }
}
