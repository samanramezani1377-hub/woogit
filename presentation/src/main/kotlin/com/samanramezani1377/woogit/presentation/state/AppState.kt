package com.samanramezani1377.woogit.presentation.state

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Offline : SyncState
    data class Pending(val count: Int) : SyncState
    data class Failed(val message: String) : SyncState
    data class Conflict(val count: Int) : SyncState
}

data class AppState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val sync: SyncState = SyncState.Idle,
)

sealed interface LoadableState<out T> {
    data object Loading : LoadableState<Nothing>
    data object Empty : LoadableState<Nothing>
    data class Content<T>(val value: T) : LoadableState<T>
    data class Error(val message: String) : LoadableState<Nothing>
    data object Offline : LoadableState<Nothing>
    data object Pending : LoadableState<Nothing>
    data object Synced : LoadableState<Nothing>
    data class Failed(val message: String) : LoadableState<Nothing>
    data class Conflict(val count: Int) : LoadableState<Nothing>
}
