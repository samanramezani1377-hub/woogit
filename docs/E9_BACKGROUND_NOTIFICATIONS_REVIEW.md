# E9 — Background & Notifications Review

## Implemented
- WorkManager CoroutineWorker boundary.
- Connected-network constraint.
- Unique periodic scheduling with KEEP policy.
- Explicit cancellation.
- No permanent connection.
- Background order source contract.
- Persistent store/order notification deduplication across process restart.
- Notification channel and order summary notification.
- PendingIntent carries store/order identifiers for order navigation.
- Network/IO failures return WorkManager retry; unexpected failures stop the worker.

## Review findings and corrections
- The initial worker was a no-op; it now executes source -> deduplication -> notification orchestration.
- Deduplication is persistent and keyed by store ID + order ID.
- The worker does not create or retain a permanent network connection.
- Notification payload contains identifiers rather than a complete order object.

## Verification status
Automated background/integration tests and the E9 Gate remain intentionally unmarked until they are executed in CI.

## Integration boundary
`OrderBackgroundSourceRegistry.source` must be supplied by the application composition root with an implementation backed by the existing repository/network boundary. This keeps background execution separate from the WooCommerce API and prevents a second network architecture.
