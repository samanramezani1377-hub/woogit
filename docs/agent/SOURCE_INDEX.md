# WooGit App — Agent Source Index

## Purpose
This is the source-of-truth locator for agent work. The contract maps in this directory describe behavior; this index identifies where executable behavior lives.

## Mandatory lookup order
1. `app/src/main/kotlin/com/samanramezani1377/woogit/` — application bootstrap, workers, security and app-level behavior.
2. `data/src/main/kotlin/com/samanramezani1377/woogit/` — network, REST transport, repositories, persistence and WooCommerce adapters.
3. `domain/src/main/kotlin/com/samanramezani1377/woogit/` — domain models/contracts/use cases.
4. `presentation/src/main/kotlin/com/samanramezani1377/woogit/` — screens, navigation, ViewModels and UI state.
5. `core/` — shared infrastructure if present.
6. Tests under the corresponding `src/test` trees are validation, not runtime source.

## Critical source anchors
| Contract area | Source anchor |
|---|---|
| App composition / dependency graph | `app/src/main/kotlin/com/samanramezani1377/woogit/AppComposition.kt` |
| Activity entry | `app/src/main/kotlin/com/samanramezani1377/woogit/MainActivity.kt` |
| Application entry | `app/src/main/kotlin/com/samanramezani1377/woogit/WooGitApplication.kt` |
| Backend HTTP contract | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/BackendClient.kt` — `verifySite`, `forward`, `forwardBinary`, `getOperation`, `revokeSession`, `revokeBillingSession`, `revokeToken`, `clearSession`, `clearBillingSession` |
| Shared HTTP transport | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/NetworkClient.kt` |
| WooCommerce transport facade | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/WooCommerceApi.kt` |
| WooCommerce provider/session boundary | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/WooCommerceClientProvider.kt` |
| Store persistence/repository | `data/src/main/kotlin/com/samanramezani1377/woogit/data/repository/StoreRepositoryImpl.kt` |
| Session persistence | `app/src/main/kotlin/com/samanramezani1377/woogit/security/AndroidBackendSessionStore.kt` |
| Secure credentials | `app/src/main/kotlin/com/samanramezani1377/woogit/security/AndroidSecureCredentialStore.kt` |
| Disconnect policy | `app/src/main/kotlin/com/samanramezani1377/woogit/security/AndroidDisconnectPolicy.kt` |
| Dashboard readiness/race handling | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/dashboard/DashboardViewModel.kt` |
| Background order polling | `app/src/main/kotlin/com/samanramezani1377/woogit/background/OrderPollingWorker.kt` |
| Product sync | `app/src/main/kotlin/com/samanramezani1377/woogit/background/ProductCatalogSyncWorker.kt` |
| Notifications | `app/src/main/kotlin/com/samanramezani1377/woogit/background/OrderNotificationManager.kt` |
| Technical error reporting | `app/src/main/kotlin/com/samanramezani1377/woogit/debug/AppTechnicalErrorReporter.kt` |

## Line-addressable contract entries
- `BackendClient.kt` → `revokeSession(storeId)` delegates to `revokeToken(sessions.get(storeId))` and clears the operational session after a successful revoke.
- `BackendClient.kt` → `revokeBillingSession(storeId)` delegates to `revokeToken(sessions.getBilling(storeId))` and clears the billing session after a successful revoke.
- `BackendClient.kt` → `revokeToken(token)` is a private suspend function returning `Result<Unit>` and POSTs `/wp-json/woogit/v1/sessions/revoke`; a missing token is a successful no-op; HTTP 401 is treated as an already-invalid/revoked session, while other non-2xx responses produce `BackendHttpException`.

## Line-addressable rule
Agents must cite the exact file and current line range when making a source-level claim. If a source file changes, refresh the relevant line ranges in the contract map. Do not copy entire source files into documentation.

## Completeness rule
When a new runtime source file is added, add it to the appropriate section of this index in the same change. A source file may be omitted from the index only when it is generated/vendor/build output and is explicitly documented as such.

## Direction of authority
`source code -> agent map`. The map never overrides executable code. A map-only change is documentation and does not require code changes.
