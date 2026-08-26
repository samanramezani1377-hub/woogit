# E0-E10 Repair Review

This pass intentionally ignores tests, as requested. The review is implementation/architecture only.

## Findings addressed

- E1/E7 repository contracts were expanded beyond single-item reads.
- E3 local persistence now has explicit domain-facing local data source contracts.
- Local-first mutation has an explicit coordination boundary so a mutation can persist locally before sync.
- E10 has typed routes, route encoding, navigation and a one-way feature state holder contract.
- App composition now has an explicit Android boundary for starting background work.

## Existing foundations verified

- SQLDelight schema contains Store, Order, Product, Variation, Attribute and Sync persistence.
- Transaction runner exists in the data layer.
- Remote data sources exist for Orders, Products and Store validation.
- Secure storage and sync queue foundations already exist.
- WorkManager background worker and notification foundations already exist.

## Remaining integration work

The repository still needs concrete implementations that bind these contracts together: local SQLDelight adapters, repository implementations combining local and remote sources, a concrete mutation coordinator, real dependency injection/composition, and the Worker-to-repository binding. These are intentionally not represented as complete merely by adding interfaces.

Tests and gates are excluded from this review by project instruction.
