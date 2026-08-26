# WooGit — Multi-Device Readiness

## V1

WooGit V1 is scoped for **one active device** per store/account.

This is a product-scope decision, not permission to build the Core around a single-device-only assumption.

## Future

The architecture should be ready for multiple users and multiple devices. The preferred future model is that **each device can operate independently** while synchronizing its changes with the shared store state.

Future requirements include:

- Stable device identity
- Per-device local state
- Durable local pending mutations
- Idempotent synchronization
- Entity/version/commit metadata that can identify the source device
- Conflict detection and resolution
- Safe concurrent updates
- Ability to revoke one device without invalidating unrelated devices

## Performance Principle

V1 must not carry the full complexity of multi-device synchronization if it is not needed yet. The Core should only preserve the identifiers, versioning and boundaries needed to add it later without replacing the local data model.

## Independence Principle

Future multi-device support should not turn devices into a shared UI session. Each device should remain locally usable and synchronize changes through explicit, reliable state transitions.
