package com.samanramezani1377.woogit.presentation.commerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeLookupResult
import com.samanramezani1377.woogit.core.domain.commerce.BarcodeResolver
import com.samanramezani1377.woogit.core.domain.commerce.CommerceFeatureEngine
import com.samanramezani1377.woogit.core.domain.commerce.InvoiceDocumentFactory
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.BulkOrderStatusResult
import com.samanramezani1377.woogit.core.domain.model.InvoiceDocument
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.sync.ProductSyncEvents
import com.samanramezani1377.woogit.data.network.BulkOrderStatusMapper
import com.samanramezani1377.woogit.presentation.PresentationErrorMapper
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 100
private const val MAX_READ_ITEMS = 10_000
private const val BATCH_SIZE = 100
private const val REFRESH_MS = 60_000L

internal data class BarcodeFeatureUiState(
    val loading: Boolean = false,
    val products: List<Product> = emptyList(),
    val orders: List<Order> = emptyList(),
    val barcodeResult: BarcodeLookupResult? = null,
    val error: String? = null,
)

internal class BarcodeFeatureViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _state = MutableStateFlow(BarcodeFeatureUiState())
    val state: StateFlow<BarcodeFeatureUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            ProductSyncEvents.updates.collect { update ->
                if (update.storeId != storeId) return@collect
                val current = _state.value.products
                val changed = update.products.associateBy { it.id.value }
                val merged = current.map { changed[it.id.value] ?: it }.toMutableList()
                update.products.forEach { product -> if (current.none { it.id.value == product.id.value }) merged.add(0, product) }
                _state.value = _state.value.copy(products = merged.distinctBy { it.id.value })
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            _state.value = _state.value.copy(loading = false, products = readProducts(), orders = readOrders())
        } catch (t: Throwable) {
            _state.value = _state.value.copy(loading = false, error = t.message)
        }
    }

    fun resolve(value: String) {
        _state.value = _state.value.copy(
            barcodeResult = BarcodeResolver.resolve(value, _state.value.products, _state.value.orders),
            error = null,
        )
    }

    private suspend fun readProducts() = readPaged { page -> dependencies.getProducts(storeId, page, PAGE_SIZE, null) }
    private suspend fun readOrders() = readPaged { page -> dependencies.getOrders(storeId, page, PAGE_SIZE, null, null) }

    private suspend fun <T> readPaged(loader: suspend (Int) -> CoreResult<List<T>>): List<T> {
        val all = mutableListOf<T>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = loader(page)) {
                is CoreResult.Success -> { all += result.value; if (result.value.size < PAGE_SIZE) break }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.toString() }
    }
}

internal data class InventoryFeatureUiState(
    val loading: Boolean = false,
    val products: List<Product> = emptyList(),
    val inventory: List<Product> = emptyList(),
    val error: String? = null,
)

internal class InventoryFeatureViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryFeatureUiState())
    val state: StateFlow<InventoryFeatureUiState> = _state.asStateFlow()
    private var query = ""
    private var lowStock = false
    private var outOfStock = false

    init {
        refresh()
        viewModelScope.launch {
            ProductSyncEvents.updates.collect { update ->
                if (update.storeId != storeId) return@collect
                val current = _state.value.products
                val changed = update.products.associateBy { it.id.value }
                val merged = current.map { changed[it.id.value] ?: it }.toMutableList()
                update.products.forEach { product -> if (current.none { it.id.value == product.id.value }) merged.add(0, product) }
                applyProducts(merged.distinctBy { it.id.value })
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        try { applyProducts(readProducts()); _state.value = _state.value.copy(loading = false) }
        catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
    }

    fun filter(nextQuery: String, nextLowStock: Boolean, nextOutOfStock: Boolean) {
        query = nextQuery
        lowStock = nextLowStock
        outOfStock = nextOutOfStock
        _state.value = _state.value.copy(inventory = CommerceFeatureEngine.filterInventory(_state.value.products, query, 5.0, lowStock, outOfStock))
    }

    private fun applyProducts(products: List<Product>) {
        _state.value = _state.value.copy(products = products, inventory = CommerceFeatureEngine.filterInventory(products, query, 5.0, lowStock, outOfStock))
    }

    private suspend fun readProducts(): List<Product> {
        val all = mutableListOf<Product>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getProducts(storeId, page, PAGE_SIZE, null)) {
                is CoreResult.Success -> { all += result.value; if (result.value.size < PAGE_SIZE) break }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.id.value }
    }
}

internal data class BulkOrdersFeatureUiState(
    val loading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val results: List<BulkOrderStatusResult> = emptyList(),
    val target: OrderStatus? = null,
    val message: String? = null,
    val error: String? = null,
)

internal class BulkOrdersFeatureViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _state = MutableStateFlow(BulkOrdersFeatureUiState())
    val state: StateFlow<BulkOrdersFeatureUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) { delay(REFRESH_MS); refresh(silent = true) }
        }
    }

    fun refresh(silent: Boolean = false) = viewModelScope.launch {
        if (!silent) _state.value = _state.value.copy(loading = true, error = null)
        try { _state.value = _state.value.copy(loading = false, orders = readOrders(), error = null) }
        catch (t: Throwable) { if (!silent) _state.value = _state.value.copy(loading = false, error = t.message) }
    }

    fun update(selectedIds: Set<String>, target: OrderStatus) = viewModelScope.launch {
        if (selectedIds.isEmpty() || target == OrderStatus.OTHER) return@launch
        val orders = _state.value.orders
        val plan = CommerceFeatureEngine.planBulkOrderStatusUpdate(orders, selectedIds, target)
        if (plan.isEmpty()) {
            _state.value = _state.value.copy(results = emptyList(), target = target, message = "همه سفارش‌های انتخاب‌شده از قبل در وضعیت «${target.faLabel()}» هستند.", error = null)
            return@launch
        }
        val client = when (val result = CommerceRuntime.provider?.commerceClient(storeId)) {
            is CoreResult.Success -> result.value.second
            is CoreResult.Failure, null -> { _state.value = _state.value.copy(error = "اتصال Commerce برقرار نیست."); return@launch }
        }
        _state.value = _state.value.copy(loading = true, target = target, message = null, error = null)
        val operationId = "bulk-orders-${System.currentTimeMillis()}"
        val results = mutableListOf<BulkOrderStatusResult>()
        plan.chunked(BATCH_SIZE).forEachIndexed { index, chunk ->
            val ids = mutableListOf<Long>()
            chunk.forEach { item -> item.orderId.toLongOrNull()?.let(ids::add) ?: results.add(BulkOrderStatusResult(EntityId(item.orderId), false, "شناسه سفارش نامعتبر است.")) }
            if (ids.isNotEmpty()) {
                val response = client.batchUpdateOrderStatuses(ids.map { it to target.name.lowercase() }, "$operationId-$index")
                results += BulkOrderStatusMapper.map(response, ids)
            }
        }
        val succeededIds = results.filter { it.succeeded }.map { it.orderId.value }.toSet()
        val updated = orders.map { if (it.id.value in succeededIds) it.copy(status = target) else it }
        val failed = results.count { !it.succeeded }
        _state.value = _state.value.copy(loading = false, orders = updated, results = results, target = target, message = "وضعیت ${results.count { it.succeeded }} سفارش به‌روزرسانی شد${if (failed > 0) "؛ $failed مورد ناموفق" else ""}.", error = if (failed > 0) "بخشی از عملیات ناموفق بود. سفارش‌های ناموفق را بررسی یا دوباره تلاش کنید." else null)
    }

    fun retryFailed() {
        val target = _state.value.target ?: return
        val ids = _state.value.results.filterNot { it.succeeded }.map { it.orderId.value }.toSet()
        if (ids.isNotEmpty()) update(ids, target)
    }

    private suspend fun readOrders(): List<Order> {
        val all = mutableListOf<Order>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getOrders(storeId, page, PAGE_SIZE, null, null)) {
                is CoreResult.Success -> { all += result.value; if (result.value.size < PAGE_SIZE) break }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.id.value }
    }
}

internal data class InvoiceFeatureUiState(
    val loading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val invoice: InvoiceDocument? = null,
    val error: String? = null,
)

internal class InvoiceFeatureViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    private val _state = MutableStateFlow(InvoiceFeatureUiState())
    val state: StateFlow<InvoiceFeatureUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { while (isActive) { delay(REFRESH_MS); refresh(true) } }
    }

    fun refresh(silent: Boolean = false) = viewModelScope.launch {
        if (!silent) _state.value = _state.value.copy(loading = true, error = null)
        try { _state.value = _state.value.copy(loading = false, orders = readOrders(), error = null) }
        catch (t: Throwable) { if (!silent) _state.value = _state.value.copy(loading = false, error = t.message) }
    }

    fun prepare(orderId: String) {
        val order = _state.value.orders.firstOrNull { it.id.value == orderId }
        if (order == null) _state.value = _state.value.copy(error = "سفارش پیدا نشد.")
        else _state.value = _state.value.copy(invoice = InvoiceDocumentFactory.create(order, "WooGit Store"), error = null)
    }

    private suspend fun readOrders(): List<Order> {
        val all = mutableListOf<Order>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getOrders(storeId, page, PAGE_SIZE, null, null)) {
                is CoreResult.Success -> { all += result.value; if (result.value.size < PAGE_SIZE) break }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.id.value }
    }
}

internal class BarcodeFeatureViewModelFactory(private val d: V1PresentationDependencies, private val s: StoreId) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(c: Class<T>): T = BarcodeFeatureViewModel(d, s) as T
}
internal class InventoryFeatureViewModelFactory(private val d: V1PresentationDependencies, private val s: StoreId) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(c: Class<T>): T = InventoryFeatureViewModel(d, s) as T
}
internal class BulkOrdersFeatureViewModelFactory(private val d: V1PresentationDependencies, private val s: StoreId) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(c: Class<T>): T = BulkOrdersFeatureViewModel(d, s) as T
}
internal class InvoiceFeatureViewModelFactory(private val d: V1PresentationDependencies, private val s: StoreId) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(c: Class<T>): T = InvoiceFeatureViewModel(d, s) as T
}
