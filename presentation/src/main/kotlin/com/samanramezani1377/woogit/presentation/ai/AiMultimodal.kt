package com.samanramezani1377.woogit.presentation.ai

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/** Shared multimodal request builders. Providers only choose the wire format their API requires. */
internal object AiMultimodal {
    fun openAiImageMessages(messages: JSONArray, attachments: List<AiAttachment>, maxImages: Int = 5, maxImageBytes: Int = 20 * 1024 * 1024): JSONArray {
        if (attachments.isEmpty()) return messages
        require(attachments.size <= maxImages) { "حداکثر $maxImages تصویر را می‌توان هم‌زمان ارسال کرد." }
        attachments.forEach { attachment ->
            require(attachment.bytes.size <= maxImageBytes) { "تصویر ${attachment.name} بزرگ‌تر از حد مجاز است." }
        }

        val result = JSONArray()
        for (i in 0 until messages.length()) {
            val message = messages.optJSONObject(i) ?: continue
            if (i != messages.length() - 1 || message.optString("role") != "user") {
                result.put(message)
                continue
            }
            val text = when (val original = message.opt("content")) {
                is String -> original
                JSONObject.NULL, null -> ""
                else -> original.toString()
            }
            val content = JSONArray().put(JSONObject().put("type", "text").put("text", text))
            attachments.forEach { attachment ->
                content.put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:${attachment.mimeType};base64,${Base64.encodeToString(attachment.bytes, Base64.NO_WRAP)}")),
                )
            }
            result.put(JSONObject(message.toString()).put("content", content))
        }
        return result
    }

    fun geminiImageParts(message: JSONObject, attachments: List<AiAttachment>, maxImageBytes: Int = 20 * 1024 * 1024): JSONArray {
        val parts = JSONArray().put(JSONObject().put("text", message.optString("content")))
        attachments.forEach { attachment ->
            require(attachment.bytes.size <= maxImageBytes) { "تصویر ${attachment.name} بیش از 20MB است." }
            parts.put(
                JSONObject().put("inlineData", JSONObject()
                    .put("mimeType", attachment.mimeType)
                    .put("data", Base64.encodeToString(attachment.bytes, Base64.NO_WRAP)))
            )
        }
        return parts
    }
}
