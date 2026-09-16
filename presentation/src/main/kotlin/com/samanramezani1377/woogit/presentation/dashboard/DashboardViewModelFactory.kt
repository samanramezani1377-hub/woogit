package com.samanramezani1377.woogit.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class DashboardViewModelFactory(
    private val dependencies: V1PresentationDependencies,
    private val storeId: StoreId,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        check(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application is unavailable for DashboardViewModel")
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(dependencies, storeId, DashboardCache(application)) as T
    }
}
