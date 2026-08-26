# WooGit V1 — Error Catalog

| Code | Meaning | Retry | User recovery |
|---|---|---:|---|
| AUTH_INVALID | Credentials rejected | No | Re-enter credentials |
| AUTH_FORBIDDEN | Credential lacks access | No | Check WooCommerce permissions |
| STORE_NOT_FOUND | Store/resource unavailable | No | Check URL/store |
| VALIDATION_ERROR | Invalid request/data | No | Fix fields |
| NETWORK_OFFLINE | No connectivity | Yes | Retry when online |
| NETWORK_TIMEOUT | Request timed out | Yes, with reconciliation for mutations | Retry |
| NETWORK_UNAVAILABLE | Temporary network failure | Yes | Retry |
| SERVER_4XX | Remote rejected request | Depends | Show actionable message |
| SERVER_409 | Remote state conflict | Reconcile | Resolve conflict |
| SERVER_429 | Rate limited | Yes/backoff | Retry later |
| SERVER_5XX | Server failure | Yes/backoff | Retry |
| MALFORMED_RESPONSE | Unexpected API payload | No | Report/diagnose |
| CONFLICT | Local change is stale | No blind retry | Resolve |
| PERMANENT_FAILURE | Operation cannot succeed automatically | No | Inspect/fix/retry manually |
| UNKNOWN | Unclassified failure | Conservative | Retry only when safe |

## Rules

1. UI messages must be human-readable and localized separately from machine error codes.
2. Retryability belongs to the domain/network mapping, not Compose code.
3. Mutating timeout is ambiguous and requires reconciliation before unsafe retry.
4. No error may silently discard a pending operation.
5. Logs must contain diagnostic identifiers but never credentials or secrets.
