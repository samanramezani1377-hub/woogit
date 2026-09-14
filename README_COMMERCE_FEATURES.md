# WooGit commerce capabilities

This document records the native architecture for the seven commerce capabilities being added to WooGit.

1. Barcode / SKU Scanner
2. Bulk Order Operations
3. Inventory Center
4. Customer Management
5. Advanced Analytics
6. Coupon Management
7. Invoice / Receipt PDF

The implementation must reuse WooGit's existing local data, repositories, pending-operation pipeline, authentication and backend forwarding. It must not copy Hippo UI or implementation patterns.

WooCommerce REST API v3 is the transport contract for new WooCommerce resources. Batch-capable resources must preserve per-entity outcomes and must not report unconditional success.
