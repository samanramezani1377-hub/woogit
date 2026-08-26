# WooGit V1 — Local Schema Contract

The concrete database technology remains an implementation decision, but its behavior must satisfy this schema contract. The database must support transactions, migrations, deterministic queries and test isolation.

## Store

- id
- name
- baseUrl
- connectionState
- credentialReference
- lastSyncAt

## Order

- storeId
- id
- status
- currency
- total
- customer snapshot/reference
- billing snapshot
- shipping snapshot
- payment summary
- shipping summary
- notes metadata
- localVersion
- serverVersion (`date_modified_gmt`)
- syncStatus
- updatedAt

## OrderItem

- storeId
- orderId
- itemId
- productId
- variationId
- name
- quantity
- subtotal/total
- tax summary
- metadata required by V1

## Product

- storeId
- id
- type
- name
- sku
- status
- description/short description as required by V1
- price fields
- stock fields
- image references
- attribute references
- localVersion
- serverVersion
- syncStatus

## Variation

- storeId
- productId
- id
- attributes
- pricing
- stock
- image reference where supported
- localVersion
- serverVersion
- syncStatus

## Attribute

- storeId
- id when global
- scope (global/custom)
- name
- values/term references

## PendingOperation

- operationId
- storeId
- entityType
- entityId
- operationType
- payload
- payloadHash
- baseServerVersion
- state
- attemptCount
- nextAttemptAt
- lastError
- createdAt
- updatedAt

## Conflict

- conflictId
- storeId
- entityType
- entityId
- operationId
- baseSnapshot
- localSnapshot
- serverSnapshot
- conflictingFields
- state
- createdAt
- resolvedAt

## SyncMetadata

- storeId
- entityType
- lastSuccessfulSyncAt
- lastKnownServerVersion
- status

## Required indexes

At minimum:

- storeId on all store-scoped entities
- order status/date
- product name/SKU where supported
- productId on variations
- operation state + nextAttemptAt
- entityType + entityId
- conflict state

## Transaction rules

Local mutation + PendingOperation creation must be atomic. Queue state transitions must be transactional. Conflict creation and mutation blocking must be atomic.

## Migration rules

Every schema change increments schema version and has an upgrade path. Destructive migration is not allowed in production V1 unless explicitly approved and accompanied by a data migration strategy.
