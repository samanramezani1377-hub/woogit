package com.samanramezani1377.woogit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class DashboardViewModelFactory(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        return DashboardViewModel(dependencies, storeId) as T
    }
}
