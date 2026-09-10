# WooGit Cross-Repository Contract Map

## Ownership

- **App repo:** `samanramezani1377-hub/woogit`, branch `main`.
- **Backend + Theme repo:** `samanramezani1377-hub/backend-site`.
- **Theme implementation branch:** `theme-v1-implementation` when Theme work is isolated there.

The Backend and Theme currently live in the same GitHub repository but are separate architectural surfaces and must remain independently mapped.

## Contract chain

`WooGit App -> WooGit REST API -> Backend Plugin -> Account/Site/Entitlement/Session -> WordPress/WooCommerce`

`Theme -> Backend/Web contracts -> WordPress/Backend`

The Theme must never consume an App Session or call `/billing/activate-session` directly. The App must never infer a Theme/web-only contract as an App API contract.

## Shared contract anchors

| Contract | App | Backend | Theme |
|---|---|---|---|
| Site identity | site/store identity models | Account + Site resolution | web site identity/session |
| Verification | `BackendClient.verifySite()` | `RestController` site verify | web verification/auth controllers |
| Billing plans | `BillingClient.plans()` | `BillingController` | pricing/subscription pages |
| Billing status | `BillingClient.status()` | `BillingController` | portal billing/subscription |
| Checkout | `BillingClient.checkout()` | `BillingController` + `BillingService` | payment/checkout web flow |
| Session lifecycle | `BackendSessionStore` | `SessionService` | web session implementation |
| Commerce proxy | `BackendClient.forward()` | `WooCommerceProxy` | Theme commerce adapter where applicable |

## Change propagation

A contract-changing code edit must update every consuming map in the affected surfaces. A map-only clarification does not authorize or require code changes.

## Source of truth

The implementation remains authoritative. Maps are navigation/contract indexes and deliberately point agents back to source files/symbols rather than replacing them.
