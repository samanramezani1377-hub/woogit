# WooGit — Authentication & Token Security

## V1 Authentication

WooGit V1 connects directly to WooCommerce through its REST API. The app must support the required WooCommerce API credentials without exposing them unnecessarily.

## Token/Credential Storage

- Consumer Key and Consumer Secret are treated as sensitive credentials.
- They must never be stored in plain text in normal app storage, logs, analytics, crash reports, screenshots, or exported configuration.
- Android secure storage backed by the platform keystore should be used for credential protection.
- Credentials must not be hard-coded into the application or repository.
- The UI should mask secrets and avoid accidental exposure through copy/share flows.

## Transport

- All WooCommerce API communication must use HTTPS/TLS.
- Cleartext HTTP must not be used for production store connections.
- Certificate/hostname validation must remain enabled; insecure TLS bypasses are prohibited.

## Credential Scope

The app should request/use only the WooCommerce API access required for the supported V1 operations. If the store administrator can create credentials with suitable permissions, the preferred configuration is the minimum practical permission level that still supports V1.

## Local Session Security

The local authenticated session must be separated from UI state. Logging out, removing a store, or explicitly revoking credentials must remove protected credentials and invalidate the local authenticated state.

## Logging

Secrets, Authorization headers, API credentials, customer authentication data, and sensitive order/customer information must never be written to logs in production.

Debug logging must use redaction and must not become a channel for credential leakage.

## Future Compatibility

The authentication layer must be provider-agnostic enough to support future improvements such as OAuth or a dedicated backend without forcing a redesign of the application domain layer.

A future backend, if introduced, must not require exposing the WooCommerce Consumer Secret to an untrusted client path.

## Security Principle

Security must not be implemented by adding unnecessary complexity to the critical user path. Credentials are protected at rest, traffic is encrypted in transit, permissions are minimized where practical, and sensitive data is excluded from logs.
