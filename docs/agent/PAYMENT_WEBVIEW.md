# App Payment WebView Return Contract

The billing flow opened from Settings uses the app-level payment WebView in `presentation/.../E11ReleaseApp.kt`.

Flow:

`BillingSection -> BillingPaymentRuntime.open(payment_url) -> payment gateway -> WooCommerce verified return -> payment-result?app=1&return_to=woogit -> app payment result view`

The app result view is intentionally user-visible and contains the explicit `بازگشت به WooGit` action. That action navigates to the same result URL with `close_app=1`.

`E11ReleaseApp` intercepts only the explicit close URL (`app=1`, `return_to=woogit`, `close_app=1`) and calls `BillingPaymentRuntime.close()`. This returns the user to the existing Settings screen.

After the WebView closes, `BillingSection` reconciles payment state through the backend and, when entitlement is active, requests the operational session. Redirect navigation itself is never considered payment proof.
