# E8 Sync Engine Review

## Implemented

- Durable pending-operation payload and deterministic payload hash storage boundary.
- Queue retrieval ordered by creation time.
- Atomic claim transition to RUNNING.
- Recovery of RUNNING operations after process death/restart.
- Retryable vs permanent failure states.
- Exponential backoff with a bounded attempt policy.
- Durable conflict snapshot storage and resolution state.
- Sync execution boundary separated from the WooCommerce API.
- Background WorkManager execution now drains the pending queue whenever connected.
- Product CREATE mutations carry a deterministic WooGit operation marker and reconcile an
  ambiguous result before issuing a second CREATE request.

## Safety decisions

- Mutations must be persisted before network execution.
- A retry must reuse the same operation id and payload hash.
- Ambiguous product CREATE failures are reconciled through the WooCommerce product metadata
  marker before another CREATE request is attempted.
- No automatic field-level merge is assumed.
- Conflict records remain user-resolvable.
- No silent overwrite of newer server state is allowed.

## Verification status

Implementation is present on `main`, but the E8 runtime test matrix is not yet claimed as passed.
The required verification remains offline mutation, reconnect, duplicate request/reconciliation,
conflict, restart/process death, and large queue behavior.
