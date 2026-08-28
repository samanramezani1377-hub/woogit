# WooGit V1 — Baseline Profile & Macrobenchmark

## Purpose

Use Android Baseline Profiles and Macrobenchmark as part of the V1 performance engineering workflow. These are development/CI quality tools; they are not runtime dependencies of the application.

## Critical user journeys

- Cold startup
- Dashboard launch
- Products launch and scrolling
- Orders launch and scrolling
- Product edit/save flow
- Navigation between primary screens
- Liquid Glass rendering and transitions

## Requirements

1. Keep benchmarks separate from the normal fast build when appropriate.
2. Measure before changing performance-sensitive code.
3. Generate and validate a Baseline Profile from representative critical user journeys.
4. Run Macrobenchmark against a release-like build.
5. Track startup, frame timing/jank, and navigation/scroll performance.
6. Do not add benchmark-only workarounds to production code.
7. Performance regressions must be investigated rather than hidden by thresholds.

## CI integration

The normal CI remains responsible for compilation, tests, lint/static checks, and release artifact generation. A dedicated performance job may run Macrobenchmark and Baseline Profile validation on an appropriate Android target so ordinary commits are not unnecessarily slowed down.

## WooGit constraints

- Local SQLDelight remains the UI data source.
- Network synchronization must remain outside composables and navigation rendering.
- Liquid Glass must be measured under realistic scrolling and navigation workloads.
- Benchmark results must represent real application behavior; no fake or synthetic success results are acceptable.
