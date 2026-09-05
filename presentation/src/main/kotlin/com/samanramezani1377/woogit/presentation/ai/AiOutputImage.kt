package com.samanramezani1377.woogit.presentation.ai

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

internal object AiOutputImageCodec {
    private const val PREFIX = "\n[[WOOGIT_OUTPUT_IMAGE|"
    private const val SUFFIX = "]]"
    private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024

    fun marker(name: String, mimeType: String, bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "تصویر خروجی خالی است." }
        require(bytes.size <= MAX_IMAGE_BYTES) { "تصویر خروجی بیش از 20MB است." }
        val safeName = name.replace("|", "_").replace("]", "_").replace("[", "_").ifBlank { "generated-image" }
        val safeMime = mimeType.takeIf { it.startsWith("image/") } ?: "image/png"
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "$PREFIX$safeName|$safeMime|$encoded$SUFFIX"
    }

    fun markerFromBase64(name: String, mimeType: String, data: String): String? = runCatching {
        marker(name, mimeType, Base64.decode(data, Base64.DEFAULT))
    }.getOrNull()

    fun extract(text: String): Pair<String, AiAttachment?> {
        val start = text.indexOf(PREFIX)
        if (start < 0) return text to null
        val end = text.indexOf(SUFFIX, start + PREFIX.length)
        if (end < 0) return text to null
        val payload = text.substring(start + PREFIX.length, end)
        val fields = payload.split('|', limit = 3)
        if (fields.size != 3) return text to null
        val image = runCatching {
            val bytes = Base64.decode(fields[2], Base64.DEFAULT)
            require(bytes.isNotEmpty() && bytes.size <= MAX_IMAGE_BYTES)
            AiAttachment(fields[0].ifBlank { "generated-image" }, fields[1].ifBlank { "image/png" }, bytes)
        }.getOrNull() ?: return text to null
        return (text.removeRange(start, end + SUFFIX.length).trim()) to image
    }

    fun appendOpenAiContentPartMarkers(builder: StringBuilder, content: Any?) {
        when (content) {
            is JSONArray -> for (i in 0 until content.length()) {
                val part = content.optJSONObject(i) ?: continue
                val image = imagePart(part) ?: continue
                builder.append(image)
            }
            is JSONObject -> imagePart(content)?.let(builder::append)
        }
    }

    fun imagePart(part: JSONObject): String? {
        val imageUrl = part.optJSONObject("image_url")
        val url = imageUrl?.optString("url").orEmpty()
        if (url.startsWith("data:image/")) {
            val comma = url.indexOf(',')
            if (comma > 0) {
                val mime = url.substring(5, url.indexOf(';', 5).takeIf { it > 0 } ?: comma)
                return markerFromBase64("generated-image", mime, url.substring(comma + 1))
            }
        }
        val b64 = part.optString("b64_json").takeIf { it.isNotBlank() }
        if (b64 != null) return markerFromBase64("generated-image", part.optString("mime_type").ifBlank { "image/png" }, b64)
        val nestedImage = part.optJSONObject("image")
        if (nestedImage != null) {
            val nestedData = nestedImage.optString("data").takeIf { it.isNotBlank() }
            if (nestedData != null) return markerFromBase64("generated-image", nestedImage.optString("mimeType").ifBlank { "image/png" }, nestedData)
        }
        return null
    }
}
