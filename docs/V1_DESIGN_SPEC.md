# WooGit V1 — Liquid Glass UI Contract

## 1. Authority

`ui-reference/liquid-glass/woogit-liquid-glass-v1-fixed.html` is the visual source of truth for WooGit V1.

A screen is **not** considered visually complete because it merely uses `WooGitTheme`, `GlassCard`, or a glass background. The actual composition, hierarchy, spacing, surfaces, navigation, controls, states and interaction treatment must follow the reference language.

When an older UI document conflicts with this contract, this contract wins for presentation decisions. Product behavior, security, WooCommerce semantics and accessibility remain mandatory constraints.

## 2. Reference visual language

The reference establishes these non-negotiable characteristics:

- soft `#EFF1F7` base background;
- four ambient mint, peach, lavender and sky blobs with slow drift;
- translucent white surfaces rather than opaque Material cards;
- backdrop blur / haze with saturation;
- bright specular top-left highlight on glass surfaces;
- thin white translucent borders;
- layered depth through soft shadow rather than hard elevation;
- large 26dp primary cards, 18dp secondary surfaces and 12dp compact controls;
- purple → pink accent gradient for primary actions;
- green live indicator and orange urgent treatment;
- compact, high-quality typography with clear ink/muted/faint hierarchy;
- rounded floating navigation rather than a conventional opaque bottom bar;
- RTL-first composition with logical spacing and trailing/leading behavior;
- content-driven card height; no arbitrary fixed-height content cards;
- vertically scrolling content area with navigation remaining visually separated from content.

## 3. Screen composition contract

Every production screen must use the same visual grammar:

```text
LiquidGlassEnvironment
  ├─ GlassTopBar
  ├─ scrollable content
  │    ├─ Section label
  │    ├─ Glass surface/card
  │    ├─ Glass surface/card
  │    └─ primary/secondary actions
  └─ floating glass navigation where navigation is available
```

Do not create a second visual system for a feature. New screens must consume the shared Liquid Glass primitives.

## 4. Required screens

### Connection

Use the reference composition for a focused glass form:

- store URL;
- Consumer Key;
- Consumer Secret;
- connection status;
- primary connect action;
- recoverable/blocking error states.

Secrets must never be exposed through logs or diagnostics.

### Dashboard

The dashboard must be a visual composition rather than a vertical stack of ordinary buttons:

- connection/live status;
- sync summary card;
- recent/urgent order card;
- compact statistics;
- quick-action glass surfaces;
- product management quick action;
- orders/product navigation;
- sync/conflict status where relevant.

### Orders

- glass search surface;
- status chips/badges;
- content-driven order cards/list rows;
- customer, total and date hierarchy;
- pending/offline/conflict indication;
- glass detail sections and mutation controls.

### Products

- glass search surface;
- prominent add-product action;
- product cards with media, title, type, SKU and stock/status hierarchy;
- product editor split into clear glass sections;
- pricing, stock, images, categories and attributes;
- variable-product and variation management.

### Settings / Sync / Conflicts

Use the same glass surfaces, state hierarchy and navigation language. Do not fall back to stock Material settings layouts.

## 5. State contract

Every screen must visually define, as applicable:

- loading;
- content;
- empty;
- offline/stale;
- recoverable error;
- blocking error;
- pending mutation;
- conflict.

States must look like intentional Liquid Glass states, not plain text appended to a screen.

## 6. Component contract

Required shared primitives:

- `GlassScaffold`
- `GlassTopBar`
- `GlassCard`
- `GlassButton`
- `GlassSecondaryButton`
- `GlassIconButton`
- `GlassTextField`
- `GlassSearchField`
- `GlassChip`
- `GlassStatusBadge`
- `GlassListItem`
- `GlassSection`
- `GlassEmptyState`
- `GlassErrorState`
- `GlassPendingState`
- `GlassOfflineState`
- `GlassSyncIndicator`
- glass dialog / sheet surfaces

If a component cannot visually express the reference treatment, the component itself must be redesigned rather than bypassed with a one-off Material implementation.

## 7. Layout rules

- All primary content is content-sized; avoid fixed-height cards.
- Vertical scrolling belongs to the content region, not the whole app shell.
- Spacing follows the reference rhythm: compact 8–12dp, section 18dp, major separation 26dp.
- Primary surfaces use 26dp radius; secondary surfaces 18dp; compact controls 12–16dp.
- Touch targets remain at least 48dp.
- RTL uses logical start/end rather than hard-coded left/right placement.
- Text, URLs, SKUs and order numbers must remain readable at system font scaling.

## 8. Accessibility and behavior

Visual fidelity never overrides accessibility, readable contrast, touch targets, offline behavior, conflict visibility or secure credential handling.

The reference is the visual target; existing WooCommerce behavior and V1 functional requirements remain intact.

## 9. Definition of UI Done

A screen is visually Done only when:

1. its structure resembles the reference rather than merely its colors;
2. all major surfaces use the shared glass treatment;
3. spacing and hierarchy follow the reference;
4. controls have the same visual language;
5. all defined states use the same system;
6. scrolling and navigation match the reference interaction model;
7. RTL and dynamic text do not break the composition;
8. no legacy opaque Material presentation remains without a documented reason.