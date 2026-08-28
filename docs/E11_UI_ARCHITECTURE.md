# WooGit E11 — UI Architecture & Modularization

## Purpose

E11 is the presentation layer for WooGit V1. It must remain modular, readable, testable and consistent with `V1_DESIGN_SPEC.md` and the Liquid Glass HTML reference.

Large monolithic or minified Kotlin files are not an acceptable final form.

## Visual Source of Truth

`ui-reference/liquid-glass/woogit-liquid-glass-v1-fixed.html`

The reference defines composition, hierarchy, spacing, glass surfaces, states, controls, scrolling and floating navigation. Reusing a glass background alone is not sufficient for visual completion.

## Architecture

```text
E11ReleaseApp.kt
    ↓
E11Routes.kt
    ↓
Screen layer
    ├── ConnectionScreen
    ├── DashboardScreen
    ├── OrdersScreen
    ├── OrderDetailScreen
    ├── ProductsScreen
    ├── ProductEditorScreen
    ├── VariationsScreen
    ├── AttributesScreen
    ├── SyncScreen
    ├── ConflictsScreen
    └── SettingsScreen
         ↓
Presentation state / callbacks
         ↓
Existing domain + data/network layer
```

## Dashboard

Dashboard presentation belongs under the dashboard package. It must consume real application state and navigation callbacks; it must never become a static mock.

Current dashboard pieces:

- `dashboard/DashboardDesign.kt`
- `dashboard/DashboardActions.kt`

The final Dashboard must reproduce the reference composition, including connection status, sync summary, recent/urgent information, compact statistics, quick actions and floating navigation.

## Shared Liquid Glass UI

Shared primitives are separated by responsibility: tokens/environment, scaffold, buttons, cards, text fields, input controls, top bar, list/status components, states, overlays and image/media primitives.

There must be one canonical implementation for each shared primitive. Screens must not introduce unrelated one-off Material styling where a shared primitive is appropriate.

## Layout Rules

- Content cards use content-driven height.
- Interactive targets are at least 48dp.
- RTL/LTR positioning uses logical `start`/`end`.
- The content region scrolls independently from persistent navigation.
- Floating navigation remains visually separated from scrollable content.
- Primary actions use the shared purple-to-pink treatment.
- Button content padding must not create visible empty bands inside the colored surface.
- Typography must remain usable with dynamic/system font scaling.

## State Contract

Relevant screens must define intentional UI for loading, content, empty, offline/stale, recoverable error, blocking error, pending mutation and conflict.

## Network/UI Boundary

Presentation code must not create ad-hoc WooCommerce network clients. Network operations remain in the data/network layer and use the established credential boundary.

WordPress Media operations specifically require the standard authenticated HTTP client, the required `Authorization`, `Content-Type` and `Content-Disposition` headers, preservation of WordPress error payloads, and distinct handling of invalid credentials versus capability errors such as `rest_cannot_create`.

A `401 rest_cannot_create` response must not automatically be presented as an invalid username/password: WordPress may have authenticated the request while denying the user's capability to create media.

## Code Quality

- No minified or intentionally single-line production Kotlin.
- One responsibility per file/component where practical.
- Prefer small composables over giant functions.
- Keep business logic out of pure visual components.
- Avoid duplicate declarations of shared primitives.
- Keep imports explicit and minimal.
- Preserve WooCommerce behavior during presentation refactoring.
- Do not claim completion based on documentation alone.
- Compilation and relevant tests are required before refactoring is considered complete.

## Definition of Done

E11 modularization is complete only when the app entry is small, navigation is centralized, screens are independently readable, Dashboard is connected to real state, shared Glass primitives are reusable and non-duplicated, no production UI source is minified, no WooCommerce capability is removed, the Dashboard matches the reference interaction model, accessibility/states are implemented, and compile/tests plus the full CI quality gate pass.

## Current Status

- [x] Canonical E11 route definitions extracted.
- [x] Shared Glass UI responsibilities split into dedicated files where implemented.
- [x] Dashboard presentation primitives extracted.
- [x] Dashboard quick actions extracted and callback-driven.
- [x] E11 architecture documentation established.
- [ ] Replace remaining monolithic E11 screen implementations.
- [ ] Connect Dashboard presentation to real application state.
- [ ] Implement reference-accurate floating navigation.
- [ ] Complete Dashboard reference states.
- [ ] Finish Media upload authentication/error tests.
- [ ] Add/finish UI tests for refactored screens.
- [ ] Run the full CI quality gate.

The checklist deliberately distinguishes implemented changes from items that still require verification.