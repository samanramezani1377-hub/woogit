# Secure Session & Entry/Exit Contract

## Baseline commit

- Commit: `f4524b8ef17833e12182d265530eae5f710100ed`
- Short SHA: `f4524b8`
- Purpose: secure app entry/exit behavior and persistent connected-store session.

## Required behavior

1. A successfully connected store remains the active authenticated store while the app is running.
2. Pressing Back from the root/dashboard does **not** disconnect the store and does not clear credentials. The task is moved to the background.
3. Pressing Back from a child screen navigates to the previous screen.
4. Reopening the app after moving it to the background returns to the authenticated store without asking for the Key/Secret again.
5. Recreating the Activity or process must restore the connected store from the persistent session source.
6. Only an explicit Disconnect action may clear the authenticated store/session and return the user to the Connection screen.
7. The Connection screen must not be used as a side effect of normal Back navigation.

## Implementation notes

- `activeStore` is navigation/runtime state; persistent session storage remains the source of truth for restoring the store.
- Do not use `rememberSaveable` as a substitute for persistent authentication/session storage.
- Do not clear credentials, call the disconnect/forget-store path, or mutate the persisted store ID from Back handling.
- Root Back uses `Activity.moveTaskToBack(true)` so the app goes to the background instead of exiting/disconnecting.

## Regression scenarios

- Dashboard → Back → reopen app → Dashboard, no Key/Secret prompt.
- Dashboard → child screen → Back → previous screen.
- Dashboard → Back → Android may later kill the process → reopen → persisted session restores the connected store.
- Settings → Disconnect → Connection screen and session cleared.

## Change control

This document records the known-good baseline for the secure entry/exit behavior. Future changes affecting authentication, session restoration, Navigation, Back handling, or Disconnect must preserve the contract above and should reference this document in the commit/PR description.
