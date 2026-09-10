# WooGit Error Map

## Transport / protocol errors

| Error | Origin | Meaning | Agent action |
|---|---|---|---|
| `Backend session is unavailable` | `BackendClient.forward()` | No operational session exists for the store | Fix readiness/session lifecycle; never send an empty session |
| HTTP 401 | Backend client | Session/credential authentication rejected | Remove affected session and use real reauthentication where supported |
| HTTP 400 | Backend/WooCommerce | Invalid request parameter or payload | Map backend/WP error code to a stable user-facing message; preserve technical detail for debug |
| Network error | HTTP stack | DNS/TLS/connectivity/transport failure | Do not report it as a backend business-rule failure |
| `CancellationException` | coroutine lifecycle | Parent operation was cancelled | Treat as cancellation side effect unless it is the root failure |

## WordPress/WooCommerce error mapping

The original WordPress/WooCommerce error identifier must remain available to the debug/technical layer. User-facing Persian messages must be unique and understandable.

Known examples:

- `rest_cannot_create` — authenticated WordPress identity lacks permission to create the requested resource.
- `rest_invalid_param` — request contains an invalid parameter; the technical field/parameter should remain visible in debug mode.

## Rule

When a new backend error code/status is introduced or its meaning changes, update this map and the corresponding App error mapper in the same change.
