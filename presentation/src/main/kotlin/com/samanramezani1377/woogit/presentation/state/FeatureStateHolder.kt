package com.samanramezani1377.woogit.presentation.state

import kotlinx.coroutines.flow.StateFlow

interface FeatureStateHolder<S, A> {
    val state: StateFlow<S>
    fun dispatch(action: A)
}

sealed interface FeatureStatus {
    data object Idle : FeatureStatus
    data object Loading : FeatureStatus
    data object Empty : FeatureStatus
    data object Offline : FeatureStatus
    data object Pending : FeatureStatus
    data object Synced : FeatureStatus
    data class Error(val message: String) : FeatureStatus
    data class Failed(val message: String) : FeatureStatus
    data class Conflict(val count: Int) : FeatureStatus
}
