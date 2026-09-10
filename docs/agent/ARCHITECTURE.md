# WooGit App Architecture Map

## 1. Runtime stack

`MainActivity -> AppComposition -> E11AppNavigation -> Screen/ViewModel -> UseCase/Repository -> Client -> Backend API`

Primary layers:

- `app/src/main/kotlin/com/samanramezani1377/woogit/` — application shell, background work and security wiring.
- `presentation/` — Compose UI, navigation and ViewModels.
- `domain/` — domain models, contracts and use cases.
- `data/` — repositories, persistence, network clients and DTOs.

## 2. Application bootstrap

1. `WooGitApplication` starts the Android application.
2. `MainActivity` hosts Compose.
3. `AppComposition.kt` constructs the shared dependency graph.
4. `AndroidBackendSessionStore` is the process/device session store.
5. `BackendClient` is the backend REST client.
6. `BillingClient` owns billing-specific backend calls.
7. Store/domain repositories consume those clients.
8. `E11AppNavigation` selects the active store and gates the UI by connection/billing state.

## 3. Store connection / login flow

`ConnectionScreen -> ConnectionViewModel.connect -> StoreRepository.connect -> BackendClient.verifySite -> POST /wp-json/woogit/v1/sites/verify -> BackendVerifyResult -> sessions.put(...) -> local credential/store persistence -> Success -> onConnected -> activeStore -> Dashboard`

The important ordering invariant is:

`verifySite response with session -> session store write -> connect success -> Dashboard refresh`

Dashboard must not issue operational backend calls before the required session exists.

## 4. Backend session ownership

`security/BackendSessionStore` is the App-side contract.

The App distinguishes:

- **Operational Session** — permits WooCommerce/store operations.
- **Billing Session** — permits billing status/checkout and can survive operational entitlement expiry until its own TTL ends.

The App must never silently treat an operational session as a billing session or vice versa.

## 5. Main data flows

### Dashboard

`E11AppNavigation -> DashboardScreen -> DashboardViewModel -> OrderRepository/ProductRepository/SalesRepository -> BackendClient.forward -> /wp-json/woogit/v1/forward -> WooCommerce REST`

### Orders

`OrdersScreen -> OrdersViewModel -> OrderRepository -> BackendClient.forward -> WooCommerceProxy`

### Products

`Product screens -> ProductRepository -> BackendClient.forward -> WooCommerceProxy`

### Settings / billing

`Settings/Billing UI -> BillingClient -> billing/plans|status|checkout|activate-session`

### Subscription expiry

`BillingClient.status -> expired -> E11AppNavigation billingLocked -> SubscriptionExpiredScreen -> plans/status -> checkout -> payment result -> status -> activateOperationalSession`

## 6. Reauthentication invariant

When an authenticated request receives a session-invalidating response, the App may remove the affected session and invoke the configured reauthentication path. Reauthentication must call the existing verification/login contract; it must not fabricate or locally mint a session.

## 7. Logout / disconnect

`Logout/Disconnect -> revoke operational session -> revoke billing session -> clear local session/credential state according to disconnect policy -> return to connection/login state`

Both session classes are part of the disconnect contract.

## 8. Background operations

Background workers include order polling, product catalog synchronization, announcements and force-update checks. Any worker that reaches WooCommerce through the backend must resolve the correct store/session context before issuing an operational request.

## 9. Source-of-truth files

- App session contract: `data/.../BackendSessionStore.kt` and `app/.../security/AndroidBackendSessionStore.kt`.
- Backend transport: `BackendClient.kt`.
- Billing transport: `BillingClient.kt`.
- Connection orchestration: `StoreRepositoryImpl.kt`, `ConnectionViewModel.kt`.
- Navigation/billing gate: `E11AppNavigation.kt`.
- Dashboard concurrency/readiness: `DashboardViewModel.kt`.
- Credential persistence: `AndroidSecureCredentialStore.kt`.

Always verify current paths/symbols before editing; this map is an index, not a substitute for source inspection.
