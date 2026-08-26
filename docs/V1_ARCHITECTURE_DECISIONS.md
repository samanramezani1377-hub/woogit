# WooGit — V1 Architecture Decisions

## Backend

V1 is explicitly **backendless**: no dedicated WooGit backend/server is required.

The primary store integration is directly through the WooCommerce REST API. A Companion WordPress/WooCommerce plugin is deferred to a future version.

## Notifications

V1 must not depend on a dedicated backend for its core operation. The notification architecture must therefore be provider-agnostic and ready for a future remote Push provider, while V1 can use Android background work and REST-based detection/notification within platform constraints.

A future provider such as FCM/OneSignal may be introduced without redesigning the notification domain, but adding such a provider does not change the V1 requirement that the app itself operates without a dedicated WooGit backend.

## Core rule

The absence of a backend must not compromise the three primary V1 goals:

1. New-order awareness
2. Fast order viewing/management
3. Fast product creation/editing

Any future backend must be an enhancement, not a prerequisite for the core product.
