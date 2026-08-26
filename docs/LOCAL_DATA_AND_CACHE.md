# WooGit — Local Data & Cache

## V1 Local Data

V1 should keep useful store data locally so normal use remains fast and resilient on weak or briefly interrupted networks.

### Cache targets

- Recent orders
- Full details of cached orders
- Products
- Full details of cached products
- Product metadata needed for editing

Large assets such as product images should use an appropriate cache policy and should not unnecessarily inflate the local database.

## Sensitive Data

Only data needed for product functionality should be persisted locally. Unnecessary sensitive information must not be cached.

Credentials are governed separately by `SECURITY_AND_AUTH.md` and must use secure credential storage rather than ordinary application data storage.

## Cache behavior

Local data is a usable representation of the latest known store state, not an independent replacement for WooCommerce in V1.

When network data is available, local state should be refreshed safely. Temporary network failure must not immediately discard valid cached data.

## Future Offline Mode

The local data layer must be suitable for a future full offline mode with durable mutations, synchronization and conflict resolution. V1 should establish these boundaries without adding unnecessary offline complexity now.
