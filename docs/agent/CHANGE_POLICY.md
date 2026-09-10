# Agent Map Change Policy

## Mandatory one-way rule

**Every code change that affects a documented contract MUST update the map in the same change.**

**A map change MUST NOT force a code change.**

This is intentionally one-way:

`CODE CHANGE -> MAP UPDATE`  REQUIRED

`MAP UPDATE -> CODE CHANGE`  NOT REQUIRED

## Contract-changing code includes

- API method/path/header/query/body/response changes.
- JSON key/type/nullability/nesting changes.
- Session creation, storage, renewal, expiry, revocation or scope changes.
- Login/logout/reconnect/verifySite changes.
- Account/Site identity or entitlement handling.
- Error codes/status/error mapping.
- Repository/domain Result/error contract changes visible to other layers.
- Persistent local data schema used by a feature.
- Billing/payment request or response behavior.
- Background operation that changes when/with which session an API is called.

## Non-contract code

Purely internal refactors that provably preserve all mapped behavior do not need a contract entry change, but the agent must verify that no mapped contract changed.

## Enforcement for agents

Before finalizing a code change, an agent MUST:

1. Identify affected map entries.
2. Compare the new source behavior with the maps.
3. Update maps in the same logical change.
4. Never edit source merely to make a stale map appear correct.

The implementation is authoritative if a map is stale; the correct action is to update the map.

## CI boundary

`docs/agent/**` is documentation/navigation only. It is excluded from WooGit CI triggers for documentation-only changes and is never packaged into an APK.
