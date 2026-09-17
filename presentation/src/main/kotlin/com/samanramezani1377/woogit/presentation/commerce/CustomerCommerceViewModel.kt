package com.samanramezani1377.woogit.presentation.commerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.presentation.PresentationErrorMapper
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class CustomerCommerceUiState(
    val orders: List<Order> = emptyList(),
    val error: String? = null,
)

internal class CustomerCommerceViewModel(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_READ_ITEMS = 10_000
        private const val REFRESH_MS = 60_000L
    }

    private val _state = MutableStateFlow(CustomerCommerceUiState())
    val state: StateFlow<CustomerCommerceUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_MS)
                refresh()
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        runCatching { readAllOrders() }
            .onSuccess { _state.value = CustomerCommerceUiState(orders = it) }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    private suspend fun readAllOrders(): List<Order> {
        val all = mutableListOf<Order>()
        var page = 1
        while (all.size < MAX_READ_ITEMS) {
            when (val result = dependencies.getOrders(storeId, page, PAGE_SIZE, null, null)) {
                is CoreResult.Success -> {
                    all += result.value
                    if (result.value.size < PAGE_SIZE) break
                }
                is CoreResult.Failure -> throw IllegalStateException(PresentationErrorMapper.message(result.error))
            }
            page++
        }
        return all.distinctBy { it.id.value }
    }
}

internal class CustomerCommerceViewModelFactory(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CustomerCommerceViewModel(dependencies, storeId) as T
}
