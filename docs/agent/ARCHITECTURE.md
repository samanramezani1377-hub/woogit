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
9. `MainActivity` installs the shared `WooCommerceClientProvider` into `CommerceRuntime` so commerce UI uses the same authenticated provider instance.

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

## 6. Commerce feature execution

Commerce has one top-level E11 surface for the customer/coupon domain and separate E11 destinations for the operational capabilities that belong elsewhere in the app:

- `E11Routes.COMMERCE -> CommerceCenterScreen` — only Customers and Coupons remain here.
- `E11Routes.COMMERCE_BARCODE -> CommerceFeatureRouteScreen(BARCODE)` — independent Barcode / SKU destination.
- `E11Routes.COMMERCE_BULK_ORDERS -> CommerceFeatureRouteScreen(BULK_ORDERS)` — independent bulk-order destination, surfaced from Orders navigation/actions.
- `E11Routes.COMMERCE_INVENTORY -> CommerceFeatureRouteScreen(INVENTORY)` — independent inventory destination, surfaced from Products navigation/actions.
- `E11Routes.COMMERCE_ANALYTICS -> CommerceFeatureRouteScreen(ANALYTICS)` — independent full analytics destination, with summary entry points allowed from Dashboard.
- `E11Routes.COMMERCE_INVOICE -> CommerceFeatureRouteScreen(INVOICE)` — independent invoice workflow destination, with entry points allowed from Order Detail.

The seven capabilities are therefore not seven cards inside Commerce Center and are not seven independent visual design systems. They share the WooGit presentation shell and design system while retaining independent feature workflows.

All feature routes use `CommerceFeatureRouteScreen`, which owns the feature ViewModel and wraps the workflow in the same `GlassScaffold` used by the main WooGit UI. `CommerceFeaturePages.kt` is content/workflow composition only; it does not own application navigation or a second app shell.

Commerce execution remains:

`CommerceFeatureRouteScreen -> CommerceFeaturePage -> CommerceViewModel -> CommerceRuntime.provider -> WooCommerceClientProvider.commerceClient -> WooCommerceCommerceApi -> BackendClient.forward -> /wp-json/wc/v3/*`

The Commerce center exposes only these flows:

- **Customer operations:** selected customers can be updated through the WooCommerce customers batch endpoint using write-only DTOs and 100-item chunks.
- **Coupon usage analytics / coupon operations:** coupon usage is aggregated from orders and selected coupons can be batch-updated through `/coupons/batch`, chunked at 100.

The independent operational flows are:

- **Barcode / SKU Scanner:** CameraX + ML Kit returns a barcode; `BarcodeResolver` resolves product SKU or order number against the loaded catalog/order set.
- **Bulk Order Status:** selected orders are planned by `CommerceFeatureEngine` and sent in WooCommerce batch requests, chunked at 100 items.
- **Inventory Filtering:** product name/SKU, low-stock and out-of-stock filtering is calculated by `CommerceFeatureEngine`.
- **Advanced Sales Analytics:** completed sales, completed/total orders, average order value, status counts, top products/customers and inventory coverage are calculated from the complete paginated product/order dataset.
- **Invoice:** `InvoiceDocumentFactory` creates the domain invoice and `InvoicePdfRenderer` renders it; Android `CreateDocument` saves the generated PDF.

Commerce reads are paginated in 100-item pages with a safety ceiling of 10,000 records per entity to avoid unbounded memory/network loops. Results are deduplicated by entity ID before analytics and UI use.

Commerce read DTOs and write DTOs remain separate so WooCommerce read-only fields are not sent back in mutations. No Commerce feature creates a second credential or session path; it uses the same authenticated `WooCommerceClientProvider` installed by `MainActivity`.

The Commerce workflows use the shared WooGit Glass presentation language. Independent routing does not permit a feature to introduce a separate scaffold, typography system, spacing system, or visual shell.

## 7. AI Agent execution

The AI write flow is confirmation-first and keeps the model as the source of the user-facing result:

`AiScreen -> AiViewModel -> AiAgent -> AiProvider -> tool_calls -> confirmation -> WooGitToolExecutor -> verified/failed result -> AiAgent outcome context -> AiProvider final response -> AiViewModel chat message`

For a multi-write response, `AiAgent` creates a `PendingBatchConfirmation`. `AiScreen` owns only selection UX; it must not fabricate a result card. After confirmation, every selected operation is executed and every rejected item is persisted as `REJECTED_BY_USER` in `AiWorkingMemoryStore`.

The batch outcome context passed back to the model contains every item and its status:

- `VERIFIED` — store mutation was confirmed.
- `FAILED` — mutation was not successfully verified; the stored error/result is authoritative.
- `REJECTED_BY_USER` — operation was not executed because it was not selected.

The model must report every item in its normal assistant response. The UI displays that response as an ordinary chat message.

`AiWorkingMemoryStore` is the recovery/audit source for execution state. It records batch item status and result, preserves `IN_FLIGHT` checkpoints, and prevents a resumed execution from treating an interrupted write as verified without checking the real store state first.

## 8. AI image operations

`WooGitToolExecutor.products_image_add` accepts an `AiAttachment`, uploads it through the WooGit media path, updates the product, then rereads the product and requires verification before returning success. A batch image operation therefore follows the same `execute -> reread -> verified` rule as other writes.

A single selected image may intentionally be reused for multiple product image-add calls. Multiple simultaneous source images must not be guessed or silently distributed across products when the mapping is ambiguous; the Agent prompt requires clarification rather than unsafe attachment reuse. The executor currently consumes the first attachment supplied to an image-add call, so explicit mapping is required before introducing automatic multi-image fan-out.

## 9. Reauthentication invariant

When an authenticated request receives a session-invalidating response, the App may remove the affected session and invoke the configured reauthentication path. Reauthentication must call the existing verification/login contract; it must not fabricate or locally mint a session.

## 10. Logout / disconnect

`Logout/Disconnect -> revoke operational session -> revoke billing session -> clear local session/credential state according to disconnect policy -> return to connection/login state`

Both session classes are part of the disconnect contract.

## 11. Background operations

Background workers include order polling, product catalog synchronization, announcements and force-update checks. Any worker that reaches WooCommerce through the backend must resolve the correct store/session context before issuing an operational request.

## 12. Source-of-truth files

- App session contract: `data/.../BackendSessionStore.kt` and `app/.../security/AndroidBackendSessionStore.kt`.
- Backend transport: `BackendClient.kt`.
- Billing transport: `BillingClient.kt`.
- Connection orchestration: `StoreRepositoryImpl.kt`, `ConnectionViewModel.kt`.
- Navigation/billing gate: `E11AppNavigation.kt`.
- Dashboard concurrency/readiness: `DashboardViewModel.kt`.
- Credential persistence: `AndroidSecureCredentialStore.kt`.
- Commerce runtime: `presentation/.../commerce/CommerceRuntime.kt`.
- Commerce navigation/UI: `presentation/.../commerce/CommerceCenterScreen.kt`, `CommerceFeatureRouteScreen.kt`, `CommerceFeaturePages.kt`, `CommerceViewModel.kt`.
- Commerce transport: `data/.../CommerceWooCommerceApi.kt`, `WooCommerceClientProvider.kt`.
- Commerce domain calculations: `core/.../commerce/CommerceFeatureEngine.kt`, `BarcodeResolver.kt`, `InvoiceDocumentFactory.kt`.
- Commerce Android scanner: `app/.../commerce/BarcodeScannerScreen.kt`, `CommerceScannerActivity.kt`.
- Commerce PDF rendering: `presentation/.../commerce/InvoicePdfRenderer.kt`.
- Commerce tests: `core/src/commonTest/kotlin/com/samanramezani1377/woogit/core/CommerceFeatureEngineTest.kt`.
- AI execution: `presentation/.../ai/AiAgent.kt`, `AiWorkingMemoryStore.kt`, `AiViewModel.kt`.
- AI tool execution: `presentation/.../ai/WooGitToolExecutor.kt`.
- AI confirmation UI: `presentation/.../ai/AiScreen.kt`.
- AI safety/reporting rules: `presentation/.../ai/AiAgentPrompt.kt`.
