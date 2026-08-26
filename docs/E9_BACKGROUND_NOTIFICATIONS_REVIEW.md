# E9 — Background & Notifications Review

## Implemented
- WorkManager CoroutineWorker boundary.
- Connected-network constraint.
- Unique periodic scheduling with KEEP policy.
- Explicit cancellation.
- Automatic pending-mutation queue drain before order polling.
- Background order source uses the existing repository/use-case boundary.
- Persistent store/order notification deduplication across process restart.
- Notification channel and order summary notification.
- PendingIntent carries store/order identifiers for order navigation.
- Network/IO failures return WorkManager retry.
- Order polling stops after a fully observed page in the normal newest-first WooCommerce polling path,
  avoiding an unbounded full-store scan on every run.

## Review findings and corrections
- The initial worker was a no-op; it now executes pending sync -> order source -> deduplication -> notification orchestration.
- Deduplication is persistent and keyed by store ID + order ID.
- The worker does not create or retain a permanent network connection.
- Notification payload contains identifiers rather than a complete order object.
- The worker uses `ExistingPeriodicWorkPolicy.KEEP` so an existing schedule is not unexpectedly replaced.

## Verification status
Automated background/integration tests and the E9 Gate remain intentionally unmarked until they are executed in CI.

## Integration boundary
The application composition root supplies the existing `GetOrders` use case to
`RepositoryOrderBackgroundSource`, keeping background execution on the same repository/network
architecture as foreground features. Pending synchronization uses the existing `SyncPendingOperations`
use case and `SyncEngine`; no second network architecture is introduced.
