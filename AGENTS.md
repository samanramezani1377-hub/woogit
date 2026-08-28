# WooGit Agent Policy — Mandatory Superpowers Workflow

## NON-NEGOTIABLE

Every AI coding agent that inspects, modifies, reviews, debugs, refactors, builds, or otherwise performs engineering work in this repository MUST use the **Superpowers** methodology from:

https://github.com/obra/superpowers

Superpowers is a required engineering workflow for this repository, not an optional suggestion.

## Before doing project work

1. Verify that Superpowers is installed/active in the current coding-agent environment.
2. Use the relevant Superpowers skills before acting. In particular:
   - `using-superpowers`
   - `brainstorming` for non-trivial changes
   - `writing-plans` before multi-step implementation
   - `systematic-debugging` for failures and bugs
   - `test-driven-development` where behavior can be tested
   - `requesting-code-review` between meaningful implementation stages
   - `verification-before-completion` before claiming success
   - `finishing-a-development-branch` when work is complete
3. Do not skip required workflow steps merely because the requested change appears small.
4. Do not declare a task complete from inspection or assumption. Verify the actual result.

## UNLAZY rule

An agent MUST continue working through the relevant implementation, build, test, review, and verification loop until the requested acceptance criteria are actually satisfied or a genuine external blocker prevents progress.

Do not:
- stop after making a partial change;
- claim success because code looks correct;
- replace a failed verification with a fake result;
- hide, disable, weaken, or bypass tests/checks to obtain green CI;
- leave known errors unresolved without explicitly reporting the blocker.

## WooGit-specific expectations

For Android/Compose work, preserve and improve the project's Local-first architecture, WooCommerce REST API constraints, synchronization correctness, performance, and Liquid Glass UI quality. Prefer evidence from builds, tests, benchmarks, source code, and real API behavior over assumptions.

For performance-sensitive Compose changes, consider stability/recomposition, lazy layouts, Baseline Profiles, and Macrobenchmark where relevant.

For WooCommerce synchronization changes, explicitly consider pagination, incremental sync, deletion reconciliation, retries/429, idempotency, conflicts, offline behavior, and API rate/hosting limitations.

## If Superpowers is unavailable

Do not silently substitute an ad-hoc workflow. State that the mandatory Superpowers prerequisite is unavailable and stop before making substantive repository changes.

## Source

Official Superpowers repository:
https://github.com/obra/superpowers
