# E7 Repository Review

## Scope
E7 connects the WooCommerce remote API and local data boundaries without adding a server. Local data remains the UI source of truth; WooCommerce remains the remote source of truth after synchronization.

## Implemented
- Order, Product and Store remote data-source boundaries.
- Explicit HTTP-to-domain error mapping boundary.
- Repository layer remains separate from Compose/UI.
- Remote data sources delegate to E6 API operations rather than duplicating HTTP logic.

## Deliberately not marked verified
- Full DTO-to-domain parsing/mapping requires concrete serialized DTOs and complete local schema coverage.
- Local-first mutation transaction + pending-operation enqueue must be completed against the final E3 queue schema before being considered verified.
- Repository integration tests remain pending by project decision.

## Review findings
The existing generic repository contracts are intentionally minimal. E7 implementation therefore introduces adapter boundaries without changing the domain contracts prematurely. Mutation idempotency, conflict reconciliation and retry execution remain E8 responsibilities.
