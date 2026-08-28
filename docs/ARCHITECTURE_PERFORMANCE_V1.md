# WooGit V1 — Local-First, Sync & UI Performance Architecture

## Goals

1. Screen navigation must not trigger a full WooCommerce catalog download.
2. Local data is the fast source for rendering; network is a synchronization source.
3. A mutation updates the local entity immediately and is represented by the durable pending-operation queue until the server confirms it.
4. Product synchronization is incremental whenever possible.
5. Heavy work runs outside the main thread.
6. UI recomposition is limited to state that actually affects the visible result.

## Runtime flow

```text
WooCommerce REST API
        │
        ├── incremental product sync (modified_after)
        ├── periodic full reconciliation
        └── mutation queue / conflict checks
        │
        ▼
   Local SQLDelight DB
        │
        ▼
 Repository / UseCase
        │
        ▼
 ViewModel / State
        │
        ▼
 Jetpack Compose + Liquid Glass
```

## Products

- `ProductRepository.list()` is local-first for an unfiltered first page.
- A screen transition therefore reads the existing local catalog instead of downloading it again.
- `ProductRepository.refresh()` is the explicit network boundary.
- `ProductCatalogSyncWorker` runs refresh in WorkManager, not during navigation.
- Normal syncs use WooCommerce `modified_after` with a small overlap window to avoid missing boundary updates.
- A full paged reconciliation runs periodically to catch remote deletions and other drift that an update-only cursor cannot detect.
- Search remains remote-backed when the local search index is not available; this avoids pretending the local cache is equivalent to the complete remote query engine.

## Mutations

Create/update/delete operations use the existing durable pending-operation architecture:

```text
User action
   ↓
Local DB update
   ↓
PendingOperation
   ↓
Immediate network attempt
   ↓
success → mark succeeded
failure → retry in background
conflict → persist conflict for explicit resolution
```

The UI must never wait for a successful remote round-trip merely to display a local mutation.

## Background execution

WorkManager is the persistent background scheduler. Product refresh and order polling are unique per store and require network connectivity. Retryable HTTP/network failures return `Result.retry()`; authentication, validation, and other permanent failures do not spin indefinitely.

## Compose performance rules

- Do not perform network I/O from composables.
- Do not make navigation callbacks responsible for complete catalog refreshes.
- Keep frequently changing scroll state out of broad recomposition; use `derivedStateOf` only when it reduces meaningful recomposition.
- Move CPU-heavy transformations to `Dispatchers.Default` when they are genuinely expensive.
- Keep I/O on `Dispatchers.IO` through repositories/workers.
- Prefer stable immutable UI state and narrow state ownership.

## Pagination

The existing repository API retains explicit `page`/`perPage` boundaries. Product background synchronization uses pages for the initial/full reconciliation. A future UI-scale catalog can introduce Paging + RemoteMediator once the local SQLDelight paging source is added; it should not be simulated with a repeated full REST download.

## API contract

WooCommerce REST v3 products supports `modified_after` and `dates_are_gmt`, which makes incremental synchronization a first-class API capability. The client must keep the cursor only after a successful sync.

## Commit discipline

Changes should remain small and reviewable:

- `feat(data): ...` — data/domain behavior
- `feat(sync): ...` — synchronization/background behavior
- `perf(ui): ...` — Compose/runtime performance
- `feat(ui): ...` — visible UI behavior
- `fix(...)` — corrective change
- `chore(build): ...` — build/dependency/CI maintenance
- `docs: ...` — documentation only

Each commit must leave the repository buildable whenever practical. CI is the gate; no fake results, skipped tests, or generated placeholder artifacts are acceptable.

## Source of truth

The local database is the rendering source of truth for cached store data. WooCommerce remains the authoritative server state. Synchronization reconciles the two and records conflicts rather than silently overwriting data.
