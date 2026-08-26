# E5 Network Foundation Review

## Scope
E5 establishes the transport foundation only. WooCommerce resource APIs remain E6.

## Implemented
- Ktor Android HTTP engine.
- Kotlinx JSON content negotiation.
- HTTPS-only request enforcement.
- WooCommerce credential injection at request time.
- No credential persistence in the normal database.
- Request timeout configuration.
- Pagination policy defaults and bounds.
- Retry classification for network failures, 408, 429 and 5xx.
- Typed mapping for authentication, permission, not-found, conflict, rate-limit and server failures.
- Cancellation is coroutine-native through Ktor suspend requests.

## Security review
- Credentials are supplied transiently to the request layer.
- Authorization is constructed at request time.
- No credential value is written to logs or database by this layer.
- HTTP URLs are rejected; no silent downgrade to HTTP.

## Deliberate V1 boundary
Automatic retry execution is not performed blindly for mutations. E5 classifies retryability; E6/E7 must apply the classification together with operation identity and idempotency/reconciliation rules.

## Verification
Tests are intentionally not marked complete yet. Required verification remains mock transport coverage for authentication failure, timeout, 4xx, 5xx and malformed payloads.
