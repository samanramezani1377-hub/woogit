package com.samanramezani1377.woogit.presentation.order

sealed interface OrderDetailSaveState {
    data object Idle : OrderDetailSaveState
    data object Saving : OrderDetailSaveState
    data object Success : OrderDetailSaveState
    data class Error(val message: String) : OrderDetailSaveState
}
