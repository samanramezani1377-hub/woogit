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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean

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
    private val refreshInFlight = AtomicBoolean(false)
    private var healthMonitorJob: Job? = null
    private var healthCheckInFlight = false

    fun refresh() {
        if (!refreshInFlight.compareAndSet(false, true)) return
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                refreshMutex.withLock { refreshInternal() }
            } finally {
                refreshInFlight.set(false)
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
        // The initial dashboard refresh is responsible for establishing readiness.
        // Do not let the background health monitor start another backend request while
        // login/navigation is still bringing the store session into the runtime store.
        if (refreshInFlight.get()) return _uiState.value.connectionState
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

    private suspend fun loadLatestOrders(): CoreResult<List<Order>> = dependencies.getOrders(storeId, 1, 30, null, null)
    private suspend fun loadLatestProducts(): CoreResult<List<Product>> = dependencies.getProducts(storeId, 1, 30, null)

    private suspend fun refreshInternal() {
        try {
            // Login/connect must finish verifySite and persist the backend session before
            // dashboard feature calls are allowed to fan out. Previously these three
            // requests ran concurrently, so salesReport could reach BackendClient.forward
            // while the session was still unavailable, producing transient login errors.
            val connectionState = checkConnection()
            if (connectionState != ConnectionState.CONNECTED) {
                _uiState.value = _uiState.value.copy(connectionState = connectionState, loading = false)
                return
            }

            val (ordersResult, productsResult) = coroutineScope {
                val ordersDeferred = async { loadLatestOrders() }
                val productsDeferred = async { loadLatestProducts() }
                awaitAll(ordersDeferred, productsDeferred)
            }.let { results ->
                results[0] as CoreResult<List<Order>> to results[1] as CoreResult<List<Product>>
            }

            val orders = when (val result = ordersResult) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> {
                    val message = PresentationErrorMapper.message(result.error)
                    PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Load orders", message, result.error.toString())
                    _uiState.value = _uiState.value.copy(connectionState = connectionState, loading = false, error = message)
                    return
                }
            }
            val products = when (val result = productsResult) {
                is CoreResult.Success -> result.value
                is CoreResult.Failure -> {
                    val message = PresentationErrorMapper.message(result.error)
                    PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Load products", message, result.error.toString())
                    _uiState.value = _uiState.value.copy(connectionState = connectionState, loading = false, error = message)
                    return
                }
            }

            _uiState.value = _uiState.value.copy(orders = orders, products = products, connectionState = connectionState, error = null)

            val metrics = coroutineScope {
                awaitAll(
                    async { dependencies.getOrders.count(storeId, null, null) },
                    async { dependencies.getOrders.count(storeId, null, "processing") },
                    async { dependencies.getSalesSummary(storeId) },
                    async { dependencies.getProducts.count(storeId, null) },
                )
            }

            val ordersTotal = (metrics[0] as CoreResult<Int>).getOrNull()
            val processingTotal = (metrics[1] as CoreResult<Int>).getOrNull()
            val salesSummaryResult = metrics[2] as CoreResult<SalesSummary>
            val productsTotal = (metrics[3] as CoreResult<Int>).getOrNull()
            val rawSalesSummary = when (salesSummaryResult) {
                is CoreResult.Success -> salesSummaryResult.value
                is CoreResult.Failure -> {
                    PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Load sales summary", PresentationErrorMapper.message(salesSummaryResult.error), salesSummaryResult.error.toString())
                    _uiState.value.salesSummary
                }
            }

            val completedOrderSum = orders.asSequence().filter { it.status.name == "COMPLETED" }.mapNotNull { it.total?.toBigDecimalOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)
            val salesSummary = rawSalesSummary?.let { summary ->
                val reported = summary.netSales.toBigDecimalOrNull()
                if (reported != null && reported.compareTo(BigDecimal.ZERO) == 0 && completedOrderSum > BigDecimal.ZERO) summary.copy(netSales = completedOrderSum.toPlainString()) else summary
            }

            val current = _uiState.value
            val newState = current.copy(ordersTotal = ordersTotal, processingTotal = processingTotal, productsTotal = productsTotal, salesSummary = salesSummary, loading = false)
            _uiState.value = newState
            DashboardSalesDebugSnapshot.update(newState.orders, salesSummary, newState.revenue)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            PresentationTechnicalErrorReporter.report("Dashboard", "DashboardViewModel.refreshInternal", "Unexpected dashboard refresh failure", "بارگذاری داشبورد با خطا مواجه شد.", e.toString())
            _uiState.value = _uiState.value.copy(loading = false, error = "بارگذاری داشبورد با خطا مواجه شد.")
        }
    }
}

private fun <T> CoreResult<T>.getOrNull(): T? = when (this) {
    is CoreResult.Success -> value
    is CoreResult.Failure -> null
}
