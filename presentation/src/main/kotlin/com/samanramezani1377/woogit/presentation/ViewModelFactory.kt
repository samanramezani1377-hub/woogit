package com.samanramezani1377.woogit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Small factory adapter used by feature composables for their real ViewModels. */
internal inline fun <reified T : ViewModel> vmFactory(crossinline creator: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            require(modelClass.isAssignableFrom(T::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return creator() as VM
        }
    }
