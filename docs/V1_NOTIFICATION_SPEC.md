# WooGit V1 — Notification Specification

## Architecture

V1 uses **no WooGit server and no remote push service**. Notifications are generated locally by the Android app after background polling of WooCommerce.

## Detection

Background work checks for new/changed orders according to Android/WorkManager scheduling constraints. The app must not assume exact execution timing; Android may defer work.

Therefore the product guarantee is best-effort notification within the documented polling/scheduling limits, not instant push.

## State

The detector persists the last known order state per store. A detected order is notified only if it has not already been acknowledged as notified for that server state.

## Deduplication key

```text
storeId + orderId + serverVersion
```

## Notification content

- store name
- order identifier
- customer/order summary where appropriate
- total where appropriate

No credential or sensitive diagnostic data is included.

## Tap behavior

```text
notification -> app -> Order Detail
```

If the order is not in local cache, the app attempts a remote fetch when online. If offline, it shows the cached state or a recoverable unavailable state.

## Permission

Android notification permission is requested at an appropriate user-facing point. If denied, the app continues functioning and exposes the notification-disabled state in settings.

## Reboot / restart

Scheduling must be restored after device reboot where Android allows it. Detector state is persisted, so restart cannot create duplicates.

## Force-stop limitation

If Android prevents background execution after a user force-stops the app, WooGit cannot guarantee background detection until the app is launched again. This is a platform limitation and must be documented rather than hidden.

## V1 limitation

There is no always-on socket, Firebase push dependency, WooGit backend, webhook relay or custom cloud notification service in V1.
