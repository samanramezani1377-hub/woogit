# WooGit Agent — Exhaustive Index

## Authority
Executable source is authoritative. This document is a locator/index, not a copy of source. For every runtime contract, record: source file, symbol, inputs, outputs, serialization, consumer, and current line range. Refresh this index whenever executable code changes.

## Runtime surfaces
- `app/src/main/kotlin/com/samanramezani1377/woogit/` — application lifecycle, workers, security, notifications.
- `data/src/main/kotlin/com/samanramezani1377/woogit/` — HTTP, Backend/WooCommerce clients, repositories, persistence.
- `domain/src/main/kotlin/com/samanramezani1377/woogit/` — domain contracts/models/use cases.
- `presentation/src/main/kotlin/com/samanramezani1377/woogit/` — screens, navigation, ViewModels/state.

## Contract discovery matrix
| ID | Search target | Primary source anchor | Map |
|---|---|---|---|
| APP-HTTP | `BackendClient`, `forward`, `verifySite` | `data/.../network/BackendClient.kt` | `API_MAP.md`, `JSON_CONTRACTS.md` |
| APP-WC | `WooCommerceApi`, `WooCommerceClientProvider` | `data/.../network/` | `API_MAP.md`, `ERROR_MAP.md` |
| APP-SESSION | `BackendSessionStore`, session headers | `app/.../security/`, `data/.../network/` | `SESSION_MAP.md` |
| APP-BILLING | `BillingClient`, `BillingGateway` | search `BillingClient` / `BillingGateway` | `API_MAP.md`, `SESSION_MAP.md` |
| APP-NAV | `E11AppNavigation`, `SubscriptionExpiredScreen` | `presentation/.../navigation/` | `ARCHITECTURE.md` |
| APP-BG | `Worker`, `WorkManager`, polling/sync | `app/.../background/` | `ARCHITECTURE.md`, `ERROR_MAP.md` |
| APP-ERROR | `BackendProtocolException`, `AppTechnicalErrorReporter` | network/debug sources | `ERROR_MAP.md` |

## JSON/HTTP exhaustive search keys
When auditing source, search all of: `@Serializable`, `Json`, `JSONObject`, `encodeToString`, `decodeFromString`, `body`, `setBody`, `Content-Type`, `Authorization`, `X-WooGit-Session`, `Idempotency-Key`, `queryParameters`, `path`, `statusCode`, `response`, `error`, `message`, `code`, `data`, `headers`.

## Required line-addressable record
Each discovered contract should eventually have this form:
`ID | file:line-line | symbol | HTTP/method | request fields | headers | response fields | error fields | consumer`

## Completeness gate
A map is incomplete if a newly added executable source file is absent from `SOURCE_INDEX.md`, or if a changed HTTP/JSON/session/error symbol has no corresponding contract entry.

## Generated/build/vendor exclusions
Do not index generated build output, Gradle caches, APK/AAB, IDE metadata, vendored binaries, or test reports as runtime source. Tests are indexed only as validation surfaces.
