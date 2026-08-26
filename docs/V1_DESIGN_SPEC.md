# WooGit V1 — Design Specification

## 1. Scope

V1 is an Android-first, offline-first WooCommerce management app. V1 has **no WooGit backend/server**. The app connects directly to the merchant's WooCommerce REST API using credentials stored in Android secure storage.

The design must support future KMP reuse without forcing V1 UI to expose future features.

## 2. Design principles

- RTL-first, LTR-ready.
- Material 3 foundation with the project's Liquid Glass visual language.
- Information density appropriate for store management.
- Every async operation has an explicit state.
- Offline state is visible but never blocks reading cached data.
- Pending local mutations are visible and actionable.
- Conflicts are never silently overwritten.
- Accessibility is a release requirement, not a later polish task.

## 3. Navigation

```text
Connection
  -> Dashboard
      -> Orders -> Order Detail -> Edit
      -> Products -> Product Detail/Edit
                       -> Variable Product -> Variations
      -> Settings
```

Notification tap:

```text
Notification -> Order Detail
```

## 4. Required screen states

Every screen must define at minimum:

- loading
- content
- empty
- offline/stale
- recoverable error
- blocking error
- pending mutation where applicable
- conflict where applicable

## 5. Core screens

### Connection
- Store URL
- Consumer Key
- Consumer Secret
- Test Connection
- Save
- connection error
- secure disconnect

Secrets must not be displayed in logs or retained in ordinary UI state longer than necessary.

### Dashboard
- connection status
- sync status
- new/recent orders
- compact order summary
- navigation to Orders and Products

### Orders
- searchable/filterable list
- status
- customer summary
- total
- date
- sync/pending indicator
- order detail
- status/edit/notes actions

### Products
- search
- list
- product detail
- create/edit/delete
- pricing
- stock
- images
- attributes
- variable product support
- variation management

### Settings
- store connection
- sync controls
- disconnect
- diagnostics that never expose credentials

## 6. Liquid Glass component rules

Components must be reusable and state-aware. The visual effect must never reduce readability or touch-target clarity.

Required primitives:

- GlassScaffold
- GlassTopBar
- GlassCard
- GlassButton
- GlassIconButton
- GlassTextField
- GlassSearchField
- GlassChip
- GlassStatusBadge
- GlassBottomSheet
- GlassDialog
- GlassListItem
- GlassSection
- GlassEmptyState
- GlassErrorState
- GlassSyncIndicator

## 7. Accessibility

- Minimum touch target: 48dp.
- Text must support system font scaling without clipping.
- Content descriptions for non-decorative icons.
- Status must not rely on color alone.
- Contrast must remain readable over glass surfaces.
- RTL layout must be verified with Persian text and mixed LTR values such as URLs, SKUs and order numbers.

## 8. Image behavior

V1 keeps the source image quality as selected by the user and does not impose an arbitrary product-image size limit. Implementation must avoid loading large images fully into memory when a streaming/file-based path is possible.

Uploads are queued when offline and retried according to Sync rules.

## 9. Design acceptance

A screen is not Done until its states, navigation, accessibility, offline behavior and mutation status are specified and implemented.
