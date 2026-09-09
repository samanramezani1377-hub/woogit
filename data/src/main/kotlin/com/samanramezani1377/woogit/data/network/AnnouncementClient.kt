package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.security.BackendSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class AnnouncementClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessions: BackendSessionStore,
    private val appVersion: String,
    private val responseObserver: BackendResponseObserver? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun getAnnouncements(storeId: String? = null): List<BackendAnnouncement> {
        val token = storeId?.let { sessions.get(it) }
        val response = httpClient.get(baseUrl.trimEnd('/') + "/wp-json/woogit/v1/announcements") {
            header("X-WooGit-App-Version", appVersion)
            token?.takeIf { it.isNotBlank() }?.let { header("X-WooGit-Session", it) }
        }
        val body = response.bodyAsText()
        responseObserver?.onResponse(response.status.value, body)
        if (response.status != HttpStatusCode.OK) throw BackendHttpException(response.status.value, body)
        val root = json.parseToJsonElement(body).jsonObject
        return root["announcements"]?.jsonArray?.mapNotNull { element ->
            val item = element.jsonObject
            val id = item["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val actions = item["actions"]?.jsonArray?.mapNotNull { actionElement ->
                val action = actionElement.jsonObject
                BackendAnnouncementAction(
                    type = action["type"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    label = action["label"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    url = action["url"]?.jsonPrimitive?.contentOrNull,
                )
            }.orEmpty()
            val image = item["image"]?.jsonObject?.let {
                BackendAnnouncementImage(
                    url = it["url"]?.jsonPrimitive?.contentOrNull,
                    alt = it["alt"]?.jsonPrimitive?.contentOrEmpty(),
                )
            }
            BackendAnnouncement(
                id = id,
                type = item["type"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                title = item["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                message = item["message"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                image = image,
                displayType = item["display_type"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1,
                actions = actions,
                dismissible = item["dismissible"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                notificationEnabled = item["notification_enabled"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                notificationType = item["notification_type"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1,
                notificationChannel = item["notification_channel"]?.jsonPrimitive?.contentOrNull ?: "announcements",
            )
        }.orEmpty()
    }
}

data class BackendAnnouncement(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val image: BackendAnnouncementImage?,
    val displayType: Int,
    val actions: List<BackendAnnouncementAction>,
    val dismissible: Boolean,
    val notificationEnabled: Boolean,
    val notificationType: Int,
    val notificationChannel: String,
)

data class BackendAnnouncementImage(val url: String?, val alt: String)

data class BackendAnnouncementAction(val type: String, val label: String, val url: String?)
