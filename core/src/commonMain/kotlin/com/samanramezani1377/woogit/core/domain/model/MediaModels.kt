package com.samanramezani1377.woogit.core.domain.model

data class MediaContent(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String,
)