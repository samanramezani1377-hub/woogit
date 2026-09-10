# WooGit App API Map

This file is the App-side endpoint index. For every entry: verify the current implementation in the referenced source before changing it.

## Endpoint matrix

| ID | Method | Path | App source | Session | Purpose |
|---|---|---|---|---|---|
| API-001 | POST | `/wp-json/woogit/v1/sites/verify` | `BackendClient.verifySite()` | none at request start | Verify WordPress/WooCommerce credentials and receive session context |
| API-002 | GET/POST/DELETE* | `/wp-json/woogit/v1/forward` | `BackendClient.forward()` | Operational | Proxy WooCommerce REST operations |
| API-003 | GET | `/wp-json/woogit/v1/billing/plans` | `BillingClient.plans()` | none | Read purchasable plans |
| API-004 | GET | `/wp-json/woogit/v1/billing/status` | `BillingClient.status()` | Billing | Read account/site billing state |
| API-005 | POST | `/wp-json/woogit/v1/billing/checkout` | `BillingClient.checkout()` | Billing | Create checkout/payment session |
| API-006 | POST | `/wp-json/woogit/v1/billing/activate-session` | `BillingClient.activateOperationalSession()` | Billing | Exchange billing access for operational access after payment |
| API-007 | POST | billing/session-revoke* | `BackendClient` session lifecycle | affected session | Revoke App-owned session during logout/disconnect |

`*` exact method/path must be confirmed from current source when editing; do not infer it from this table.

## API-001 — verifySite

Flow:

`ConnectionViewModel.connect -> StoreRepositoryImpl.connect -> BackendClient.verifySite -> REST -> parse BackendVerifyResult -> put operational/billing sessions -> return Success`

Request must carry the site identity and WordPress/WooCommerce credentials required by the backend contract. The response is expected to contain:

- `account_id`
- `site_id`
- `session`
- `scope`
- `billing_session` when supplied by the backend
- `access_enabled`
- `billing_required`

The App must not mark a store connected until the required session has been persisted.

## API-002 — forward

`BackendClient.forward()` resolves the operational session from `BackendSessionStore` and sends it as `X-WooGit-Session`.

If the operational session is missing, the client fails with `BackendProtocolException("Backend session is unavailable")`. This is intentional: do not replace this with an empty token or an unauthenticated request.

A 401 invalidates/removes the affected operational session and enters the configured reauthentication path where supported.

## API-003 — billing/plans

Plans are readable without an operational session. This endpoint is required for the expired-subscription UI and therefore must remain usable when commerce access is disabled.

## API-004 — billing/status

Uses the Billing Session. It must not depend on the operational entitlement remaining active.

Expected billing response fields consumed by the App include:

- `status`
- `starts_at`
- `expires_at`
- `capabilities`
- `trial_used`

The App interprets `status == "expired"` as billing-locked operational UI state.

## API-005 — billing/checkout

Uses the Billing Session and sends an idempotency key. A checkout request must not require the expired operational session.

The returned checkout/payment URL is consumed by the billing WebView flow.

## API-006 — billing/activate-session

Uses a Billing Session. Successful activation returns a new operational session and scope. The App stores the new operational session and removes the consumed billing session when the backend contract says the billing token has been exchanged.

## Headers

Contractually important headers include:

- `X-WooGit-Session` — App session token when required.
- `Idempotency-Key` — checkout and other mutation operations where required by the backend contract.
- Standard HTTP `Content-Type` / `Accept` according to the serializer/client implementation.

## Request construction rule

Do not hand-write JSON in UI code. Request DTO/serializer ownership must remain in the data/network layer. If a request field changes, update `JSON_CONTRACTS.md` and the backend contract map in the same code change.
