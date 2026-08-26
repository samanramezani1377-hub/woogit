# E1 — Core Foundation Review

## Scope

This review covers the implementation surface for E1.01–E1.10. Test/CI gates are intentionally not marked as passed yet.

## Dependency direction

- Core contracts live under `core/src/commonMain`.
- No Core source imports Compose, Android UI, WorkManager, or presentation modules.
- Store-scoped contracts are defined in Core.

## Identity and time

- `EntityId` and `StoreId` are non-blank value classes.
- `kotlinx.datetime.Instant` is the shared timestamp representation.
- V1 server-version boundary uses `date_modified_gmt`.

## Domain errors

- Typed `DomainError` classification exists.
- Recoverable/non-recoverable behavior is explicit.
- Presentation mapping is represented by stable keys rather than UI dependencies.

## Repository and use-case contracts

- Repository contracts are model-agnostic so E2 domain models can bind them without changing Core boundaries.
- Operations are store-scoped where store data is involved.
- Order, product, connection and sync use-case contracts exist.

## Events and notifications

- Event publisher/subscriber boundaries are defined.
- Notification intents/provider boundary exists.
- No notification provider implementation is introduced in E1.

## Future boundaries

- Multi-Store-ready store scope is present without activating Multi-Store behavior.
- Assistant/AI is a boundary only; no implementation is included.
- Future commit versioning remains behind a provider boundary.

## Review result

Implementation review: **PASS**.

Build/test/CI verification is intentionally **not marked complete** in this document; those checks remain separate gates as requested for the V1 execution process.
