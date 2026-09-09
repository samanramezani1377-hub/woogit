package com.samanramezani1377.woogit.core.domain

import kotlin.coroutines.CoroutineContext

/** Marks a read as cache-only. Repositories must not start remote refreshes while active. */
object CacheOnlyRead : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key
    object Key : CoroutineContext.Key<CacheOnlyRead>
}
