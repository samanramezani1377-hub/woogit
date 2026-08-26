# WooGit V1 — Permission & Security Matrix

## Android permissions

| Permission/capability | Purpose | Required | Denied behavior |
|---|---|---:|---|
| INTERNET | WooCommerce API communication | Yes | App cannot sync; cached data remains available |
| POST_NOTIFICATIONS | New-order notifications | No | App works; notification feature is disabled |
| Network state | Decide when background sync is useful | As platform capability | Fall back to normal scheduling |

The implementation must not request unrelated storage permissions merely to manage product images. Use modern Android file/photo APIs and temporary app-private files where possible.

## Credentials

- WooCommerce Consumer Key/Secret are secrets.
- Store them only in Android secure storage.
- Never persist them in normal DB tables.
- Never put them in URLs when a secure supported authentication mechanism is available.
- Never log them.
- Never include them in crash reports.
- Clear them on disconnect.

## Transport

- HTTPS required for remote store connections.
- Certificate/TLS failures are errors, not reasons to silently downgrade to HTTP.
- No custom WooGit server exists in V1.

## Logging

Allowed diagnostic fields:

- operationId
- entity type/id where safe
- error code
- retry count
- timestamps
- coarse network state

Never log:

- consumer secret
- consumer key in full
- Authorization headers
- raw credential payloads
- full customer private data unless strictly required for a controlled debug build

## Backup

Secrets must be excluded from ordinary application backup. Database contents must be reviewed for privacy implications before release.
