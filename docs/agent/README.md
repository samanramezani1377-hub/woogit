# WooGit Agent Architecture Map

This directory is the machine-readable/agent-readable contract map for the Android App.

## Mandatory rule

**CODE -> MAP is mandatory. MAP -> CODE is not mandatory.**

Whenever an agent changes application code, it MUST update the affected map/contract in the same change. A documentation-only map change MUST NOT require a code change.

The map is descriptive, not authoritative over source code. Source code is the implementation source of truth; this map is the navigation and contract index.

## Files

- `ARCHITECTURE.md` — module/layer ownership and file-level flow.
- `API_MAP.md` — endpoint-by-endpoint App -> Backend calls, headers, auth/session, consumers and source symbols.
- `JSON_CONTRACTS.md` — request/response JSON keys and serialization ownership.
- `SESSION_MAP.md` — login, verifySite, reconnect, logout, Billing Session and Operational Session lifecycle.
- `ERROR_MAP.md` — backend/network/domain errors and their App mappings.
- `CROSS_REPO_MAP.md` — App ↔ Backend ↔ Theme contract references.
- `api-index.json` — compact machine-readable endpoint index for agents.
- `CHANGE_POLICY.md` — mandatory update rules and CI enforcement model.

## Search rule

An agent should search this directory first for an API path, JSON key, session scope, error code, class name or feature name, then open the referenced source file and verify the implementation.

## Coverage rule

Every externally observable API contract, JSON field, session transition, error mapping, persistence contract and user-visible flow that is changed by code must have a corresponding map entry.

Generated or copied source code must never be treated as the map. The map points back to source paths and symbols so it remains useful without duplicating the implementation.
