# Coupon management

## Individual create / edit

`CommerceCenterScreen -> CommerceFeaturePage -> CouponsPage -> CommerceViewModel`

Coupon management is intentionally individual-only. The page has no bulk coupon editing UI.

## Create

`CouponsPage -> onCreateCoupon -> CommerceViewModel.createCoupon -> WooCommerceCommerceApi.createCoupon`

Creation is optimistic/local-first at the presentation state boundary: the new coupon is inserted into the local Commerce state with a temporary negative ID before the WooCommerce request is sent. A successful response replaces that temporary item with the remote coupon. A failed request rolls the local insertion back and exposes an error.

The write contract is `WooCouponCommerceWriteDto`; no backend-specific contract or intermediate server storage is introduced.

## Edit

`CouponsPage -> onEditCoupon -> CommerceViewModel.updateCoupon -> WooCommerceCommerceApi.updateCoupon`

Edit is also optimistic: the local coupon is updated first and restored on a failed remote update.
