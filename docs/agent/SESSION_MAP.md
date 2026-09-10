# WooGit Session Lifecycle Map

## Session classes

| Scope | Stored by | Used by | Expiry relationship |
|---|---|---|---|
| `operational` | `BackendSessionStore` / `AndroidBackendSessionStore` | WooCommerce proxy/operational data | tied to entitlement |
| `billing` | separate Billing Session storage | billing status/checkout/activation | independent short-lived billing access |

## Verify

`verifySite -> backend validates WordPress/WooCommerce + Account/Site -> response -> App stores primary session + dedicated billing session -> connect success`

Never reorder the final two operations. A connected store without the required session is an invalid App state.

## Login/reconnect

`stored credentials -> verifySite -> refresh session state -> publish connected store -> start operational consumers`

Reauthentication must use real credentials/verification and the backend must re-check Account + Site + Entitlement. The App never manufactures a valid backend session.

## Operational expiry

`operational session invalid/expired -> remove operational token -> reauthenticate or billing lock -> do not send operational calls without token`

## Subscription expiry

`billing/status -> status=expired -> operational UI locked -> SubscriptionExpiredScreen -> billing/plans/status -> checkout -> payment confirmation -> billing/activate-session -> new operational session`

The payment page must not depend on an already-expired operational session.

## Billing session exchange

`Billing Session -> /billing/activate-session -> backend validates billing scope + entitlement -> Operational Session -> App stores operational token`

The consumed Billing Session may be removed after successful exchange according to the current backend contract.

## Logout/disconnect

Both operational and billing tokens are revoked/cleared. Local credential clearing follows the disconnect policy; revoking a token must not be confused with deleting the site/account.

## Race prevention

Dashboard and background consumers must not start operational calls before session readiness. `DashboardViewModel` must establish/confirm readiness before `refresh()` reaches `BackendClient.forward()`.

## Source symbols

- `BackendSessionStore`
- `AndroidBackendSessionStore`
- `BackendClient.verifySite`
- `BackendClient.forward`
- `BillingClient.status`
- `BillingClient.checkout`
- `BillingClient.activateOperationalSession`
- `StoreRepositoryImpl.connect`
- `ConnectionViewModel.connect`
- `E11AppNavigation`
- `SubscriptionExpiredScreen`
