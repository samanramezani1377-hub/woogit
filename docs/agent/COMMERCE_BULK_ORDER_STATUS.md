# Commerce — Bulk Order Status

## Purpose

Bulk Order Status is an independent WooGit Commerce route for store staff to select multiple WooCommerce orders and change their status in one operation.

The route remains separate from the Commerce Center. The intended entry point is from the Orders experience.

## UI contract

- Liquid Glass shell: shared `GlassTopBar`, `GlassCard`, `GlassSearchField`, `GlassPrimaryAction`, and `GlassOutlinedButton` components.
- Search by order number.
- Filter by all standard order statuses supported by the domain.
- Multi-selection persists when the filter changes.
- `انتخاب نتایج` adds all visible orders to the selection; `پاک کردن` clears the whole selection.
- Confirmation shows the effective number of orders that will actually change.
- The UI does not expose `OrderStatus.OTHER` as a write target because `other` is not a standard WooCommerce REST order status.
- After a batch operation, successful and failed order IDs are shown. Failed items can be retried without reselecting the whole list.

## Data contract

`CommerceViewModel.bulkOrderStatus` plans only selected orders whose current status differs from the target. Requests are split into WooCommerce-compatible batches of at most 100 orders.

Each batch receives a stable operation/chunk idempotency key:

`commerce-orders-<operation-id>-<chunk-index>`

The response is decoded by `BulkOrderStatusMapper`. A 2xx response is not treated as an automatic success for every requested order: each requested order must appear in the returned `update` array. Missing items are reported as individual failures.

Non-numeric WooCommerce order IDs are rejected individually instead of being silently dropped from the request.

## Refresh behavior

After a successful or partially successful operation, only the order collection and order-derived analytics are refreshed. Products, customers, and coupons are not reloaded unnecessarily.

## Failure/retry behavior

- Full HTTP failure: every requested order in that batch is marked failed with the response body/error.
- Partial/missing response item: only the missing order is marked failed.
- UI exposes a retry action for failed order IDs using the same target status.
- The final operation message reports exact success/failure counts.

## Tests

`data/src/test/kotlin/com/samanramezani1377/woogit/data/network/BulkOrderStatusMapperTest.kt` covers:

1. successful + missing items in the same 2xx batch response;
2. HTTP-level batch failure with the WooCommerce error body.

The agent-map contract is intentionally updated in the same atomic change as the validation test so the guard evaluates the code/docs pair together.
