package com.samanramezani1377.woogit.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AnalyticsUiState(
    val loading: Boolean = false,
    val range: AnalyticsRange = AnalyticsRange.YEAR,
    val analytics: AnalyticsSnapshot? = null,
    val error: String? = null,
)

internal class AnalyticsViewModel(
    private val storeId: StoreId,
) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    fun load(range: AnalyticsRange = _state.value.range) = viewModelScope.launch {
        val loader = AnalyticsRuntime.loader
        _state.value = _state.value.copy(loading = true, range = range, error = null)
        if (loader == null) {
            _state.value = _state.value.copy(loading = false, error = "منبع تحلیل محلی آماده نیست.")
            return@launch
        }
        when (val result = loader(storeId, range)) {
            is CoreResult.Success -> _state.value = _state.value.copy(
                loading = false,
                range = range,
                analytics = result.value,
                error = null,
            )
            is CoreResult.Failure -> _state.value = _state.value.copy(
                loading = false,
                range = range,
                error = result.error.toString(),
            )
        }
    }
}

internal class AnalyticsViewModelFactory(
    private val storeId: StoreId,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AnalyticsViewModel(storeId) as T
}
