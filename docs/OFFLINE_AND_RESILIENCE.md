# WooGit — Offline & Network Resilience

## V1 Goal

V1 does not require a dedicated backend and does not promise a fully autonomous offline product. However, **normal use over weak, unstable, or briefly interrupted internet must remain usable**.

A short network interruption must not make the application unusable, discard user actions, or force the user to restart the workflow.

## Network Resilience

The app must be designed around intermittent connectivity:

- Local state should remain available while requests are pending.
- Requests should use timeouts, retry/backoff and cancellation appropriately.
- Temporary failures must be distinguishable from permanent API/authentication errors.
- User actions that can be safely queued should enter a persistent Pending Queue rather than being lost.
- When connectivity returns, pending operations should resume automatically.
- Repeated retries must be idempotent where possible and must not create duplicate products, updates, commits or notifications.
- UI should show a clear pending/sync state without blocking unrelated local navigation.

## Local-first Foundation

The V1 architecture should establish the foundation for a future full offline mode. Local data and pending mutations should be modeled so that a future backend can extend offline capabilities without replacing the core domain model.

## Future Full Offline Mode

A future version may support meaningful operation with no active store connection through a stronger local cache, durable mutation queue, conflict handling and eventually a dedicated backend. This is preparation only in V1; it must not add unnecessary complexity or performance cost now.

## Critical Principle

**Brief internet loss is a normal operating condition, not a fatal application state.**

The three primary V1 workflows — new-order awareness, order management and product management — must degrade gracefully under weak/unstable connectivity.
