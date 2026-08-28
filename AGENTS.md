# WooGit Agent Policy — Mandatory Agent Engineering Stack

## NON-NEGOTIABLE

Every AI coding agent that inspects, modifies, reviews, debugs, refactors, builds, tests, or otherwise performs engineering work in this repository MUST use ALL of the following applicable engineering resources:

1. **Superpowers** methodology:
   https://github.com/obra/superpowers
2. **Addy Osmani Agent Skills**:
   https://github.com/addyosmani/agent-skills
3. **Official Kotlin Agent Skills** from the Kotlin organization:
   https://github.com/Kotlin/kotlin-agent-skills
4. **Android Agent Skills**:
   https://github.com/new-silvermoon/awesome-android-agent-skills
5. **WooGit CI Verification** is a mandatory acceptance gate for repository changes.

These are required engineering workflows for WooGit, not optional suggestions.

## Before doing project work

1. Verify that Superpowers is installed/active in the current coding-agent environment.
2. Verify that Addy Osmani Agent Skills are available/installed and use the applicable skills for the task.
3. Verify that the official Kotlin Agent Skills are available/installed and use any applicable Kotlin skill before Kotlin/Android/Gradle implementation.
4. Verify that the Android Agent Skills are available/installed and use the applicable Android/Compose/testing skill for Android work.
5. Use the relevant Superpowers workflow for planning, implementation, debugging, review, testing, and verification.
6. Do not skip a required applicable resource merely because the requested change appears small.
7. Do not declare completion from inspection or assumption. Verify the actual result.

## Skill responsibilities

- **Superpowers:** structured problem solving, brainstorming, planning, systematic debugging, TDD, code review, verification, and finishing work.
- **Addy Osmani Agent Skills:** production engineering practices, implementation quality, review discipline, performance, accessibility, testing, and shipping workflows where applicable.
- **Official Kotlin Agent Skills:** Kotlin language/tooling/Gradle-specific guidance and migrations where applicable.
- **Android Agent Skills:** Android, Jetpack Compose, UI, architecture, testing, emulator/QA, and Android build workflows where applicable.

For overlapping guidance, use the most directly applicable authoritative skill while preserving all repository requirements.

## CI Verification — MANDATORY

Every substantive repository change MUST be verified through the project's actual CI workflow.

Required gates must remain enabled and must pass:
- Debug build
- Release build
- Unit tests
- Android lint
- APK verification
- Final CI gate

The agent MUST:
1. Push the implementation.
2. Wait for the GitHub Actions run triggered by the implementation.
3. Inspect the actual CI result and relevant logs.
4. If CI fails, diagnose the real failure, fix it, push again, and re-run verification.
5. Repeat until all required CI gates are green, or stop only when a genuine external blocker prevents verification and report it explicitly.

Never:
- remove tests to make CI green;
- skip required builds/checks;
- weaken quality gates;
- use `continue-on-error` to hide failures;
- fake a successful CI result;
- declare success before the actual CI result is green.

## WooGit-specific expectations

For Android/Compose work, preserve and improve the project's Local-first architecture, WooCommerce REST API constraints, synchronization correctness, performance, and Liquid Glass UI quality. Prefer evidence from builds, tests, benchmarks, source code, and real API behavior over assumptions.

For performance-sensitive Compose changes, consider stability/recomposition, lazy layouts, Baseline Profiles, and Macrobenchmark where relevant.

For WooCommerce synchronization changes, explicitly consider pagination, incremental sync, deletion reconciliation, retries/429, idempotency, conflicts, offline behavior, and API rate/hosting limitations.

## If a required resource is unavailable

If an applicable required resource cannot be made available in the current coding-agent environment, report that prerequisite before making substantive changes rather than silently substituting an unrelated workflow.

## Sources

Superpowers:
https://github.com/obra/superpowers

Addy Osmani Agent Skills:
https://github.com/addyosmani/agent-skills

Official Kotlin Agent Skills:
https://github.com/Kotlin/kotlin-agent-skills

Android Agent Skills:
https://github.com/new-silvermoon/awesome-android-agent-skills
