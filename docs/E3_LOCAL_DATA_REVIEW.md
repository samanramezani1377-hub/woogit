# E3 Local Data Review

## Scope
E3 establishes the V1 local persistence foundation using SQLDelight on Android.

## Decisions
- SQLDelight is the database layer.
- The first schema version is created by SQLDelight's generated schema.
- Store, order, product, variation, attribute and pending-operation data are persisted locally.
- Secrets are represented only by credential references; raw credentials are not stored in domain tables.
- Pending operations are durable and have explicit retry metadata.
- A process restart converts `RUNNING` operations back to `PENDING`; terminal states remain available for reconciliation.
- Transaction execution is kept behind a dedicated boundary.

## Review findings
- Data depends on Core, never the reverse.
- SQLDelight is isolated in the Data module.
- Store scoping is present on all mutable business tables.
- Version/modified timestamps are persisted for future reconciliation.
- No UI models or UI dependencies are introduced.

## Verification status
Implementation review: PASS.
Build/DB integration/migration tests: intentionally not marked complete yet; they remain part of the later verification pass.
