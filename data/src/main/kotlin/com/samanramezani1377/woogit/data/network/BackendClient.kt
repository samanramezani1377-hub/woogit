package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.debug.NoOpTechnicalErrorReporter
import com.samanramezani1377.woogit.core.debug.TechnicalErrorContext
import com.samanramezani1377.woogit.core.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.core.security.BackendSessionStore
import com.samanramezani1377.woogit.core.security.CredentialPair
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.security.MessageDigest

class BackendClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val credentials: SecureCredentialStore,
    private val sessions: BackendSessionStore,
    private val appVersion: String,
    private val technicalErrorReporter: TechnicalErrorReporter = NoOpTechnicalErrorReporter,
    private val responseObserver: BackendResponseObserver? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun verifySite(storeId: String, siteUrl: String, pair: CredentialPair): Result<BackendVerifyResult> = runCatching {
        val credentialDigest = sha256("${pair.consumerKey}\u0000${pair.consumerSecret}\u0000${pair.wordpressUsername.orEmpty()}\u0000${pair.wordpressApplicationPassword.orEmpty()}".toByteArray(Charsets.UTF_8))
        val idempotencyKey = "verify-${sha256("$storeId\u0000$siteUrl\u0000$credentialDigest".toByteArray(Charsets.UTF_8))}"
        val endpoint = "/wp-json/woogit/v1/sites/verify"
        val response = httpClient.post(url(endpoint)) {
            header("X-WooGit-App-Version", appVersion)
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("url", JsonPrimitive(siteUrl))
                put("wordpress_username", JsonPrimitive(pair.wordpressUsername.orEmpty()))
                put("wordpress_application_password", JsonPrimitive(pair.wordpressApplicationPassword.orEmpty()))
                put("consumer_key", JsonPrimitive(pair.consumerKey))
                put("consumer_secret", JsonPrimitive(pair.consumerSecret))
            })
        }
        val body = response.bodyAsText()
        responseObserver?.onResponse(response.status.value, body)
        if (response.status.value !in 200..299) {
            val exception = BackendHttpException(response.status.value, body, extractBackendReason(body))
            technicalErrorReporter.report(TechnicalErrorContext(feature="Store", location="BackendClient.verifySite", operation="Verify site", type="BackendHttpError", httpMethod="POST", endpoint=endpoint, httpStatus=response.status.value.toString(), responseBody=body, details="WooGit Backend returned a non-success HTTP response"), exception)
            throw exception
        }
        val obj = json.parseToJsonElement(body).jsonObject
        val token = obj["session"]?.jsonPrimitive?.contentOrNull ?: throw BackendProtocolException("Missing Backend session")
        val scope = obj["scope"]?.jsonPrimitive?.contentOrNull ?: ""
        val accessEnabled = obj["access_enabled"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val billingToken = obj["billing_session"]?.jsonPrimitive?.contentOrNull
        if (scope == "billing") {
            sessions.remove(storeId)
            sessions.putBilling(storeId, token)
        } else {
            sessions.put(storeId, token)
            if (billingToken != null) sessions.putBilling(storeId, billingToken)
        }
        BackendVerifyResult(token, scope, accessEnabled, billingToken)
    }.onFailure { throwable ->
        if (throwable !is BackendHttpException) reportTransport("Store", "BackendClient.verifySite", "POST", "/wp-json/woogit/v1/sites/verify", throwable)
    }

    suspend fun forward(storeId: String, path: String, method: String, pair: CredentialPair, query: Map<String, Any> = emptyMap(), body: String? = null, idempotencyKey: String? = null): ApiResponse {
        val endpoint = "/wp-json/woogit/v1/forward?path=$path"
        return try {
            val token = sessions.get(storeId) ?: throw BackendProtocolException("Backend session is unavailable")
            val normalizedMethod = method.uppercase(); val key = if (normalizedMethod in MUTATION_METHODS) idempotencyKey ?: stableMutationKey(storeId, normalizedMethod, path, query, body) else null
            val response = httpClient.request(URLBuilder(url("/wp-json/woogit/v1/forward")).apply { parameters.append("path", path); query.forEach { (k,v) -> parameters.append(k,v.toString()) } }.build()) {
                this.method=HttpMethod.parse(normalizedMethod); header("X-WooGit-App-Version",appVersion); header("X-WooGit-Session",token)
                header("X-WooGit-Consumer-Key",pair.consumerKey); header("X-WooGit-Consumer-Secret",pair.consumerSecret)
                pair.wordpressUsername?.takeIf{it.isNotBlank()}?.let{header("X-WooGit-Wordpress-Username",it)}
                pair.wordpressApplicationPassword?.takeIf{it.isNotBlank()}?.let{header("X-WooGit-Wordpress-Application-Password",it)}
                key?.let{header("Idempotency-Key",it)}; if(body!=null){contentType(ContentType.Application.Json);setBody(body)}
            }
            val text=response.bodyAsText();responseObserver?.onResponse(response.status.value,text);if(response.status.value==401)sessions.remove(storeId)
            ApiResponse(response.status.value,text,normalizedMethod,path,response.headers.entries().associate{it.key.lowercase() to it.value.joinToString(",")})
        }catch(throwable:Throwable){reportTransport("WooCommerce","BackendClient.forward",method.uppercase(),endpoint,throwable);throw throwable}
    }

    suspend fun forwardBinary(storeId: String, path: String, method: String, pair: CredentialPair, query: Map<String, Any> = emptyMap(), bytes: ByteArray, contentType: String, fileName: String, idempotencyKey: String? = null): ApiResponse {
        val endpoint="/wp-json/woogit/v1/forward?path=$path"
        return try{
            val token=sessions.get(storeId)?:throw BackendProtocolException("Backend session is unavailable");val normalizedMethod=method.uppercase();require(normalizedMethod in MUTATION_METHODS){"Binary forwarding is only supported for mutations"}
            val payloadDigest=sha256(bytes);val key=idempotencyKey?:stableMutationKey(storeId,normalizedMethod,path,query,"binary:$payloadDigest")
            val response=httpClient.request(URLBuilder(url("/wp-json/woogit/v1/forward")).apply{parameters.append("path",path);query.forEach{(k,v)->parameters.append(k,v.toString())}}.build()){
                this.method=HttpMethod.parse(normalizedMethod);header("X-WooGit-App-Version",appVersion);header("X-WooGit-Session",token);header("X-WooGit-Consumer-Key",pair.consumerKey);header("X-WooGit-Consumer-Secret",pair.consumerSecret)
                pair.wordpressUsername?.takeIf{it.isNotBlank()}?.let{header("X-WooGit-Wordpress-Username",it)};pair.wordpressApplicationPassword?.takeIf{it.isNotBlank()}?.let{header("X-WooGit-Wordpress-Application-Password",it)};header("Idempotency-Key",key);header(HttpHeaders.ContentDisposition,"attachment; filename=\"${fileName.substringAfterLast('/').substringAfterLast('\\')}\"");this.contentType(ContentType.parse(contentType));setBody(bytes)
            }
            val text=response.bodyAsText();responseObserver?.onResponse(response.status.value,text);if(response.status.value==401)sessions.remove(storeId);ApiResponse(response.status.value,text,normalizedMethod,path,response.headers.entries().associate{it.key.lowercase() to it.value.joinToString(",")})
        }catch(throwable:Throwable){reportTransport("Media","BackendClient.forwardBinary",method.uppercase(),endpoint,throwable);throw throwable}
    }

    suspend fun getOperation(storeId:String,operationId:String):BackendOperationStatus=runCatching{
        val token=sessions.get(storeId)?:throw BackendProtocolException("Backend session is unavailable");val response=httpClient.get(url("/wp-json/woogit/v1/operations/${operationId.urlEncode()}")){header("X-WooGit-App-Version",appVersion);header("X-WooGit-Session",token)};val body=response.bodyAsText();responseObserver?.onResponse(response.status.value,body);if(response.status.value==401)sessions.remove(storeId);if(response.status.value !in 200..299)throw BackendHttpException(response.status.value,body,extractBackendReason(body));val obj=json.parseToJsonElement(body).jsonObject;BackendOperationStatus(obj["operation_id"]?.jsonPrimitive?.contentOrNull?:operationId,obj["status"]?.jsonPrimitive?.contentOrNull?:"unknown",obj["response"]?.toString())
    }.onFailure{throwable->reportTransport("Sync","BackendClient.getOperation","GET","/wp-json/woogit/v1/operations/$operationId",throwable)}.getOrThrow()

    suspend fun revokeSession(storeId:String):Result<Unit> = revokeToken(sessions.get(storeId)).also{ if(it.isSuccess)sessions.remove(storeId) }

    suspend fun revokeBillingSession(storeId:String):Result<Unit> = revokeToken(sessions.getBilling(storeId)).also{ if(it.isSuccess)sessions.removeBilling(storeId) }

    private suspend fun revokeToken(token:String?):Result<Unit>=runCatching{
        if(token==null)return@runCatching Unit
        val response=httpClient.post(url("/wp-json/woogit/v1/sessions/revoke")){header("X-WooGit-App-Version",appVersion);header("X-WooGit-Session",token)}
        val body=response.bodyAsText();responseObserver?.onResponse(response.status.value,body);if(response.status.value !in 200..299&&response.status.value!=401)throw BackendHttpException(response.status.value,body,extractBackendReason(body))
    }.onFailure{throwable->reportTransport("Session","BackendClient.revokeSession","POST","/wp-json/woogit/v1/sessions/revoke",throwable)}

    fun clearSession(storeId:String){sessions.remove(storeId);sessions.removeBilling(storeId)}
    fun clearBillingSession(storeId:String)=sessions.removeBilling(storeId)

    private fun extractBackendReason(body:String):String?=runCatching{val obj=json.parseToJsonElement(body).jsonObject;obj["reason"]?.jsonPrimitive?.contentOrNull?:obj["code"]?.jsonPrimitive?.contentOrNull?:obj["message"]?.jsonPrimitive?.contentOrNull}.getOrNull()
    private fun reportTransport(feature:String,location:String,method:String,endpoint:String,throwable:Throwable){technicalErrorReporter.report(TechnicalErrorContext(feature=feature,location=location,operation="Backend HTTP request",type="NetworkError",httpMethod=method,endpoint=endpoint.substringBefore('?'),details="Backend transport/protocol request failed"),throwable)}
    private fun stableMutationKey(storeId:String,method:String,path:String,query:Map<String,Any>,body:String?):String{val canonical="$storeId|$method|$path|${query.toSortedMap().entries.joinToString("&"){"${it.key}=${it.value}"}}|${body.orEmpty()}";return "app-${sha256(canonical.toByteArray(Charsets.UTF_8))}"}
    private fun sha256(value:ByteArray):String=MessageDigest.getInstance("SHA-256").digest(value).joinToString(""){"%02x".format(it)}
    private fun String.urlEncode():String=java.net.URLEncoder.encode(this,Charsets.UTF_8.name()).replace("+","%20")
    private fun url(path:String)=baseUrl.trimEnd('/')+path
    private companion object{val MUTATION_METHODS=setOf("POST","PUT","PATCH","DELETE")}
}

data class BackendVerifyResult(val session:String,val scope:String,val accessEnabled:Boolean,val billingSession:String?=null)
data class BackendOperationStatus(val operationId:String,val status:String,val responseBody:String?)
class BackendHttpException(val statusCode:Int,val responseBody:String,val backendReason:String?=null):RuntimeException("WooGit Backend HTTP $statusCode${backendReason?.let{" : $it"}?:""}")
class BackendProtocolException(message:String):RuntimeException(message)
