# WooGit Agent Policy — Mandatory Agent Engineering Stack

## Instruction priority — USER REQUESTS COME FIRST

For every AI coding agent working in this repository, instruction precedence is:

1. **Higher-priority platform/system/developer safety and execution rules** — always apply.
2. **The current user's explicit request** — this has priority over repository documentation and any pre-existing project instruction when they conflict, as long as it does not conflict with a higher-priority rule.
3. **This repository documentation (`AGENTS.md`, `CLAUDE.md`, `.github/copilot-instructions.md`, and other project docs)** — follow it when it does not conflict with the current user's request.
4. **Agent defaults, assumptions, preferences, or generic workflow habits.**

### Critical rule

**Never use repository documentation to override, reject, reinterpret, or silently replace an explicit current user instruction.** If the user asks for a different implementation, workflow, scope, or priority than this documentation describes, follow the user's instruction and adapt the repository workflow accordingly.

If the user's instruction is ambiguous, ask for clarification rather than inventing a conflicting requirement from project documentation. If there is a genuine conflict with a higher-priority platform/system/developer rule, follow that higher-priority rule and explain the limitation when necessary.

This precedence rule applies to all AI coding agents, including Claude, Copilot, Codex, and future agents.

## NON-NEGOTIABLE ENGINEERING RESOURCES

Every AI coding agent that inspects, modifies, reviews, debugs, refactors, builds, tests, or otherwise performs engineering work in this repository MUST use all applicable engineering resources below, **unless the current user explicitly requests otherwise**:

1. **Superpowers** methodology:
   https://github.com/obra/superpowers
2. **Addy Osmani Agent Skills**:
   https://github.com/addyosmani/agent-skills
3. **Official Kotlin Agent Skills** from the Kotlin organization:
   https://github.com/Kotlin/kotlin-agent-skills
4. **Android Agent Skills**:
   https://github.com/new-silvermoon/awesome-android-agent-skills
5. **WooGit CI Verification** as the normal acceptance gate.

These are repository defaults, not instructions that outrank the current user.

## Before doing project work

1. Check applicable project resources and skills.
2. Follow the user's current request as the source of truth for the requested outcome and scope.
3. Use relevant repository workflows where they do not conflict with the user's request.
4. Do not declare completion from inspection or assumption when verification is requested or required.

## CI Verification

For substantive changes, CI is the normal acceptance gate:

- Debug build
- Release build
- Unit tests
- Android lint
- APK verification
- Final CI gate

The normal workflow is:
1. Implement the requested change.
2. Push it when the user requests a push or the agreed workflow calls for one.
3. Inspect the actual GitHub Actions result.
4. If CI fails, diagnose and fix the real failure rather than hiding it.

**Important:** This CI workflow is subordinate to the user's explicit instructions. If the user explicitly asks to delay CI, skip a run, make a documentation-only change without CI, or use another verification order, honor that request unless a higher-priority rule requires otherwise.

Never remove tests, weaken quality gates, use `continue-on-error` to hide failures, fake results, or claim that a verification step passed when it did not.

## WooGit-specific expectations

For Android/Compose work, preserve and improve the project's Local-first architecture, WooCommerce REST API constraints, synchronization correctness, performance, and Liquid Glass UI quality where applicable and consistent with the user's request.

For WooCommerce synchronization changes, explicitly consider pagination, incremental sync, deletion reconciliation, retries/429, idempotency, conflicts, offline behavior, and API rate/hosting limitations where relevant.

## If a required resource is unavailable

If an applicable resource cannot be made available, report that limitation only when it materially affects the requested work. Do not use an unavailable optional resource as a reason to override an explicit user instruction.

## Sources

Superpowers:
https://github.com/obra/superpowers

Addy Osmani Agent Skills:
https://github.com/addyosmani/agent-skills

Official Kotlin Agent Skills:
https://github.com/Kotlin/kotlin-agent-skills

Android Agent Skills:
https://github.com/new-silvermoon/awesome-android-agent-skills
