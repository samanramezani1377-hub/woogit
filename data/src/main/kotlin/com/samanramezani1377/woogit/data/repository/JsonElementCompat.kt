package com.samanramezani1377.woogit.data.repository

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal val JsonElement.contentOrNull: String?
    get() = (this as? JsonPrimitive)?.content
