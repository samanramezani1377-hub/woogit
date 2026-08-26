package com.samanramezani1377.woogit.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface FeatureAction

abstract class FeatureStateHolder<S, A : FeatureAction>(initialState: S) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected fun reduce(transform: (S) -> S) = _state.update(transform)

    abstract fun onAction(action: A)
}
