# E3 Local Data Remediation

## Scope
This remediation closes the implementation gaps found during the V1 E0-E3 audit.

## Changes
- SQLDelight remains the V1 Android-compatible local database implementation.
- Added persisted global and custom attribute tables and queries.
- Added SQLDelight migration files for the initial schema and attribute schema upgrade.
- Removed the unsupported parameterized `PRAGMA user_version` query from the `.sq` schema.
- Added an explicit fail-closed migration failure policy; V1 never silently discards business data on migration failure.
- Kept transaction execution behind `DatabaseTransactionRunner`.
- Kept all mutable business data store-scoped.

## Architecture decision
V1 remains Android-first for the concrete SQLDelight driver while Core contracts stay KMP-compatible. A future iOS/native driver can be introduced behind the Data boundary without moving database concerns into Domain.

## Verification status
Implementation remediation: complete.
Automated build, database integration, migration, restart, and invalid-state tests are intentionally not marked complete yet and remain verification work.
