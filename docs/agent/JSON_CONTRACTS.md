# WooGit JSON Contract Map

## JSON-001 — Site verification response

Consumed by `BackendClient.verifySite()`.

```json
{
  "account_id": "integer",
  "site_id": "integer",
  "session": "string",
  "scope": "operational|billing",
  "billing_session": "string|null",
  "access_enabled": "boolean",
  "billing_required": "boolean"
}
```

`session` is the primary session for the verification result. When access is enabled it is operational; when access is disabled it is billing. `billing_session` is the dedicated billing token when the backend supplies one.

## JSON-002 — Billing status

Consumed by `BillingClient.status()`.

```json
{
  "billing": {
    "status": "string",
    "starts_at": "string|null",
    "expires_at": "string|null",
    "capabilities": "object|array|null",
    "trial_used": "boolean|null"
  }
}
```

The exact nested shape must be checked against the current `BillingClient` parser and `BillingController` before a field is added/removed.

## JSON-003 — Billing checkout response

The checkout parser consumes the backend's checkout object and payment URL. The exact keys are owned by `BillingClient.checkout()` and the backend `BillingController::checkout()` response. Never invent a field name in UI code.

## JSON-004 — Operational session activation response

Consumed by `BillingClient.activateOperationalSession()`.

Expected contract:

```json
{
  "session": "string",
  "scope": "operational"
}
```

The App stores the returned operational token only after validating the response.

## JSON-005 — Backend error envelope

All backend errors must be mapped from the actual HTTP status + backend error code/message into App domain errors. The original technical error may remain available to debug/technical reporting, but user-facing Persian text must be stable and unique.

Typical fields encountered by the App contract include:

```json
{
  "code": "string",
  "message": "string",
  "data": "object|null"
}
```

The exact envelope is source-controlled by the backend; App code must not assume undocumented fields.

## JSON search keys

Agents should search for these literals first:

- `session`
- `billing_session`
- `scope`
- `access_enabled`
- `billing_required`
- `status`
- `starts_at`
- `expires_at`
- `capabilities`
- `trial_used`
- `code`
- `message`
- `data`
- `X-WooGit-Session`
- `Idempotency-Key`

## Change rule

A JSON key is a contract. Adding, removing, renaming, changing nullability, changing type, or moving a key requires:

1. App source update.
2. This file update.
3. Backend/Theme contract map update when that side consumes or produces the field.
4. Relevant tests updated or added.
