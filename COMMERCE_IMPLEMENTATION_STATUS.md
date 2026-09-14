# Commerce implementation status

The seven commerce capabilities are being integrated into `main` using WooGit's existing architecture.

## Scope

- Barcode / SKU Scanner
- Bulk Order Operations
- Inventory Center
- Customer Management
- Advanced Analytics
- Coupon Management
- Invoice / Receipt PDF

## Rules

- `main` only.
- No Hippo UI or implementation copying.
- Reuse existing authentication, backend forwarding, local persistence and pending operations.
- No fake CI or bypassed tests.
- Every batch operation must expose per-item failures.
