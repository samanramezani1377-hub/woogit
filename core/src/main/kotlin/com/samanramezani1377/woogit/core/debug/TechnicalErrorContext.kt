package com.samanramezani1377.woogit.core.debug

/** Transport/domain-safe technical context. No credentials or Authorization headers are stored. */
data class TechnicalErrorContext(
    val feature: String,
    val location: String,
    val operation: String,
    val type: String = "TechnicalError",
    val httpMethod: String = "",
    val endpoint: String = "",
    val httpStatus: String = "",
    val responseBody: String = "",
    val details: String = "",
)

fun TechnicalErrorContext.withHttp(
    method: String,
    url: String,
    status: String = "",
    body: String = "",
): TechnicalErrorContext = copy(
    httpMethod = method,
    endpoint = url.substringBefore('?'),
    httpStatus = status,
    responseBody = body,
)
