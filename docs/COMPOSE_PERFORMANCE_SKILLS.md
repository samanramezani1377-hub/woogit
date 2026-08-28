# Compose Performance Skills

WooGit uses the upstream `skydoves/compose-performance-skills` repository as a pinned engineering reference for Jetpack Compose performance. It is intentionally kept as a Git submodule under `tools/compose-performance-skills` rather than packaged into the application.

## Pinned revision

`7cc35c8c03c9ab820dd37d3067f080e1abbd3234`

## How it is used

Before changing performance-sensitive Compose code, consult the relevant upstream skill. The project should follow the repository's measurement-first approach:

1. Measure the real symptom.
2. Diagnose stability/recomposition/layout/build causes.
3. Apply the smallest justified fix.
4. Verify with release-mode measurement where applicable.

Relevant areas for WooGit include stability, recomposition, lazy lists, modifiers, side effects, measurement, Baseline Profiles, R8, and end-to-end performance auditing.

## Important constraint

This repository is an engineering reference and agent-skill source, not an Android runtime dependency. Do not copy its libraries into the APK merely because a skill mentions them. Any runtime library must be evaluated separately against WooGit's architecture and build constraints.

The upstream project follows the Agent Skills standard and is intended to give coding agents operational Compose-performance guidance rather than act as a conventional Android library.

Upstream: https://github.com/skydoves/compose-performance-skills
