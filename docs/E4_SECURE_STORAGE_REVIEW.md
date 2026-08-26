# E4 — Secure Storage Review

## Scope
V1 WooCommerce Consumer Key/Secret handling on Android.

## Decisions
- Domain persists only `CredentialReference`; raw credentials are never part of domain entities.
- Android credentials are encrypted with an AES-256 key held by Android Keystore.
- Encrypted ciphertext and IV are persisted in app-private SharedPreferences; the plaintext secret is never persisted there.
- `allowBackup=false` is enabled on the Android application manifest.
- Disconnect deletes the credential ciphertext, IV, and Keystore key, then marks the StoreConnection disconnected.
- V1 retains cached store data on disconnect; credential cleanup is mandatory and independent from data retention.
- Diagnostic logging exposes only safe metadata and has no credential-bearing fields.
- HTTPS enforcement remains a Network-layer responsibility and is not bypassed by credential storage.

## Verification status
Implementation review completed. Security tests are intentionally not marked as passed until the Android test suite is executed in CI/device.
