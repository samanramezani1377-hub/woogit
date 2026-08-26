# WooGit V1 — Pre-Build Gate

This gate must pass before production feature implementation begins.

## Locked V1 architecture

- Android-first app.
- Kotlin Multiplatform-compatible Core boundaries.
- Jetpack Compose + Material 3 foundation.
- Ktor Client boundary.
- WorkManager for background polling.
- Local-first data and persisted pending queue.
- WooCommerce REST API is the only remote system.
- **No WooGit backend/server in V1.**
- No custom WooGit cloud database.
- No custom push service.
- No required WooCommerce companion plugin.

## Database decision

V1 uses **SQLDelight** for the local database because the architecture explicitly requires KMP-compatible core/data reuse, typed SQL/schema ownership, transactions and migrations. Android remains the only V1 shipped platform, but the data layer is not intentionally coupled to Android-only database APIs.

This is an implementation decision for V1, not a restriction on future versions.

## Locked contracts

The following documents are normative for V1:

- `V1_DESIGN_SPEC.md`
- `V1_DATA_CONTRACTS.md`
- `V1_API_CONTRACT.md`
- `V1_SCHEMA.md`
- `V1_NOTIFICATION_SPEC.md`
- `V1_ERROR_CATALOG.md`
- `V1_PERMISSION_SECURITY.md`
- `V1_TRACEABILITY.md`

## Pre-build checks

- [ ] Hi-Fi design covers every V1 screen and state.
- [ ] Navigation and deep-link behavior are defined.
- [ ] SQLDelight schema matches `V1_SCHEMA.md`.
- [ ] API operations match `V1_API_CONTRACT.md`.
- [ ] Sync state machine matches `V1_DATA_CONTRACTS.md`.
- [ ] Mutation idempotency/reconciliation strategy is implemented as a contract before retry code.
- [ ] Conflict behavior has no silent overwrite path.
- [ ] Notification behavior explicitly documents Android scheduling limitations.
- [ ] Credentials are isolated from normal persistence/logging.
- [ ] Traceability links every critical requirement to implementation and tests.

## Release-blocking principles

A V1 build must not be released if any of these are true:

- a pending mutation can disappear silently;
- a retry can silently duplicate an unsafe mutation;
- a server conflict can silently overwrite local/server data;
- a credential can appear in ordinary logs/database/crash payloads;
- notification behavior is advertised as instant push while V1 has no push backend;
- a critical V1 requirement has no test/evidence path.
