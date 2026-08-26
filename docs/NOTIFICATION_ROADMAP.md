# WooGit — Notification Roadmap

## V1 Priority

**New Order Created** is the primary and highest-priority push notification event.

When a new order is registered, the user should receive the explicit order notification defined in `NOTIFICATION_SPEC.md`, even when the app is closed.

## Future Notification Events

Other order lifecycle events can be added later, including status changes and other important updates.

The Event Core and notification architecture must remain extensible so these events can be enabled without redesigning the V1 notification path.

## Settings

Future notification settings may allow users to choose which event types they receive. Optional notification features must never reduce the reliability or speed of the New Order notification.

## Priority Rule

```text
New Order Created     → V1 / Highest priority
Order Status Changes  → Future / Optional
Other Events          → Future / Configurable
```
