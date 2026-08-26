# WooGit V1 — WooCommerce API Contract

## Boundary

WooGit V1 talks directly to the merchant's WooCommerce REST API. There is no WooGit server.

## Connection

Connection validation must verify:

- HTTPS URL
- reachable WooCommerce API
- credentials accepted
- required resources accessible

Credentials are never included in logs.

## Orders

Required operations:

- list orders
- retrieve order
- filter/search/sort orders
- paginate orders
- update supported order fields/status
- add order notes

Every response is mapped through DTO -> Domain mapping. UI never consumes API DTOs directly.

## Products

Required operations:

- list products
- retrieve product
- search products
- create product
- update product
- delete product
- variable product support

## Variations

Required operations:

- list variations
- retrieve variation
- create variation
- update variation
- delete variation

## Attributes

Required V1 operations depend on the product editing contract and must support the attribute data needed for simple and variable products. Global attributes/terms and custom product attributes must remain distinguishable.

## Images

Product main image and gallery management is supported through WooCommerce. No WooGit image backend is introduced.

## Batch operations

Where WooCommerce supports batch operations for product mutations, WooGit may use them for V1 bulk/compound workflows. Each logical local operation must remain traceable and failures must be represented per affected entity; a batch response must never be treated as an unconditional all-success result.

## Pagination

Remote lists must expose page/per-page or cursor-equivalent information to the repository layer. Local UI pagination must not require direct knowledge of WooCommerce query syntax.

## Error mapping

HTTP and transport errors map to the Domain error taxonomy:

- 400/422 -> VALIDATION_ERROR where applicable
- 401 -> AUTH_INVALID
- 403 -> AUTH_FORBIDDEN
- 404 -> STORE_NOT_FOUND or entity-not-found semantics
- 409 -> conflict semantics where applicable
- 429 -> retryable rate limit
- 5xx -> retryable server failure
- timeout/network -> retryable network failure
- malformed payload -> MALFORMED_RESPONSE

## Mutation safety

A timeout after a mutation request is an ambiguous outcome. WooGit must not blindly duplicate an unsafe mutation. It must reconcile state where practical before retrying.

## Compatibility

The implementation must target the documented WooCommerce REST resources actually required by V1 and keep endpoint-specific DTOs isolated from Domain models. API assumptions must be covered by integration tests against a real or controlled WooCommerce test instance.
