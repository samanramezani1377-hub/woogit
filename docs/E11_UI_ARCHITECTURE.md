# WooGit E11 — UI Architecture & Modularization

## Purpose

E11 is the presentation layer for the WooGit V1 Android application. Its implementation must remain modular, readable and consistent with `V1_DESIGN_SPEC.md` and the Liquid Glass HTML reference.

The goal is to keep screen composition, navigation, state handling and shared visual primitives separate. Large monolithic or minified Kotlin files are not an acceptable final form.

## Source of Visual Truth

The visual source of truth is:

`ui-reference/liquid-glass/woogit-liquid-glass-v1-fixed.html`

Presentation decisions must follow the shared Liquid Glass contract. Existing WooCommerce behavior, security, accessibility and offline requirements remain mandatory.

## Module Responsibilities

### App entry

`E11ReleaseApp.kt`

Responsible only for application-level composition and wiring. It must not contain the complete implementation of every screen.

### Navigation

`E11Routes.kt`

Contains the canonical E11 destinations and route definitions. Screens receive navigation callbacks instead of hard-coding navigation inside reusable UI components.

### Screens

Each production screen should have its own file or a small, clearly related group of files:

- `ConnectionScreen.kt`
- `DashboardScreen.kt`
- `OrdersScreen.kt`
- `OrderDetailScreen.kt`
- `ProductsScreen.kt`
- `ProductEditorScreen.kt`
- `VariationsScreen.kt`
- `AttributesScreen.kt`
- `SyncScreen.kt`
- `ConflictsScreen.kt`
- `SettingsScreen.kt`

Additional files may be introduced when a screen contains a genuinely independent feature such as media selection.

### Dashboard

Dashboard presentation belongs under the dashboard package. The dashboard is composed from reusable Liquid Glass surfaces and receives real application state and navigation callbacks.

Current modular dashboard presentation files include:

- `dashboard/DashboardDesign.kt`
- `dashboard/DashboardActions.kt`

The dashboard must not be replaced by a static mock. Displayed values, connection state and actions must come from the application/domain state.

### Shared Glass UI

Shared visual primitives belong under the glass component package and are split by responsibility. They must remain multi-line, readable Kotlin source.

Current responsibilities include:

- tokens and environment
- scaffold
- buttons
- cards
- text fields
- input controls
- top bar
- list/status components
- states
- overlays
- image/media primitives

No screen should recreate the glass system with unrelated one-off Material styling when an appropriate shared primitive exists.

## Layout Rules

- Content cards are content-driven; do not use arbitrary fixed heights for content.
- Minimum interactive touch target is 48dp.
- Use logical `start`/`end` for RTL/LTR behavior.
- Main content scrolls independently from persistent navigation.
- Floating navigation remains visually separated from the scrollable content.
- Primary actions use the shared purple-to-pink Liquid Glass treatment.
- Primary button content padding must not create visible empty bands inside the colored surface.
- Button positions, widths and surrounding spacing must not change when correcting the visual height of an existing action.

## State Rules

Every relevant screen must account for:

- loading
- content
- empty
- offline/stale
- recoverable error
- blocking error
- pending mutation
- conflict

State rendering belongs in presentation components and must not leak network implementation details into reusable UI.

## Network/UI Boundary

Screens must not construct ad-hoc network clients for WooCommerce operations. Network requests belong to the data/network layer and receive credentials through the established credential boundary.

For WordPress Media operations specifically:

- use the project's standard authenticated HTTP client;
- send the required `Authorization` header;
- send the correct `Content-Type`;
- send the correct `Content-Disposition`;
- preserve WordPress error information;
- distinguish invalid credentials from capability errors such as `rest_cannot_create`.

A `401 rest_cannot_create` response must not automatically be treated as an invalid username/password. It can mean that the authenticated WordPress user lacks the capability required to create media.

## Code Quality Rules

- No minified or intentionally single-line production Kotlin.
- One responsibility per file/component where practical.
- Prefer small composables over giant functions.
- Keep business logic out of pure visual components.
- Avoid duplicate declarations of the same visual primitive.
- Keep imports explicit and minimal.
- Preserve existing behavior while refactoring presentation.
- Refactoring is not complete until the project compiles and relevant tests pass.

## Definition of Done for E11 Modularization

1. App entry is small and readable.
2. Navigation is centralized.
3. Screens are independently readable.
4. Dashboard is independently composable and connected to real state.
5. Shared Glass primitives are reusable and non-duplicated.
6. No production UI source is minified or intentionally single-line.
7. No functional WooCommerce behavior is removed during UI refactoring.
8. UI tests/compile checks pass before the refactor is considered complete.
9. Documentation reflects the actual implementation rather than planned-only work.

## Current Refactor Status

- [x] Canonical E11 route definitions extracted.
- [x] Shared Glass UI responsibilities split into dedicated files where implemented.
- [x] Dashboard presentation primitives extracted.
- [x] Dashboard quick actions extracted and callback-driven.
- [ ] Replace remaining monolithic E11 screen implementations.
- [ ] Connect Dashboard presentation to real application state.
- [ ] Implement reference-accurate floating navigation.
- [ ] Complete all Dashboard reference states.
- [ ] Add/finish UI tests for the refactored screens.
- [ ] Run the full CI quality gate.

This status intentionally distinguishes completed refactoring from work that still requires verification.
