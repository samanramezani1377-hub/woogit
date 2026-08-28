# WooGit Agent Policy — Mandatory Superpowers + Kotlin Agent Skills Workflow

## NON-NEGOTIABLE

Every AI coding agent that inspects, modifies, reviews, debugs, refactors, builds, tests, or otherwise performs engineering work in this repository MUST use both:

1. **Superpowers** methodology:
   https://github.com/obra/superpowers
2. **Kotlin Agent Skills** from the official Kotlin organization:
   https://github.com/Kotlin/kotlin-agent-skills

These are required engineering workflows for WooGit, not optional suggestions.

## Before doing project work

1. Verify that Superpowers is installed/active in the current coding-agent environment.
2. Verify that the official Kotlin Agent Skills are available/installed in the current coding-agent environment.
3. Use the relevant Superpowers skills before acting. In particular:
   - `using-superpowers`
   - `brainstorming` for non-trivial changes
   - `writing-plans` before multi-step implementation
   - `systematic-debugging` for failures and bugs
   - `test-driven-development` where behavior can be tested
   - `requesting-code-review` between meaningful implementation stages
   - `verification-before-completion` before claiming success
   - `finishing-a-development-branch` when work is complete
4. For Kotlin/Android/Gradle engineering, use the relevant **Kotlin Agent Skills** in addition to Superpowers. Prefer the official Kotlin skills over ad-hoc Kotlin guidance whenever a matching skill exists.
5. Do not skip required workflow steps merely because the requested change appears small.
6. Do not declare a task complete from inspection or assumption. Verify the actual result.

## Kotlin Agent Skills — mandatory usage

For every Kotlin-related task, the agent MUST first check the official Kotlin Agent Skills collection and activate/use any applicable skill before implementation.

Official source:
https://github.com/Kotlin/kotlin-agent-skills

Current official skill categories include Kotlin tooling and backend skills. Relevant examples include:
- `kotlin-tooling-agp9-migration` for Android Gradle Plugin 9 migration work
- `kotlin-tooling-java-to-kotlin` for Java-to-Kotlin migration work
- `kotlin-tooling-immutable-collections-0-5-x-migration` for the corresponding immutable-collections migration
- `kotlin-tooling-native-build-performance` for Kotlin/Native build-performance work

If no official Kotlin Agent Skill matches the task, continue using Superpowers and the repository's existing engineering rules; do not invent a claim that a Kotlin skill was used.

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

## If either required methodology is unavailable

If Superpowers is unavailable, or if the applicable official Kotlin Agent Skills cannot be made available in the current coding-agent environment, do not silently substitute an ad-hoc workflow. State the missing prerequisite before making substantive repository changes.

## Sources

Official Superpowers repository:
https://github.com/obra/superpowers

Official Kotlin Agent Skills repository:
https://github.com/Kotlin/kotlin-agent-skills
