# WooGit API Sandbox

A local, deterministic WooCommerce REST API sandbox for safe WooGit integration and UI testing.

## Goals

- Never touches a real WooCommerce store.
- Exposes WooCommerce-compatible `/wp-json/wc/v3/*` endpoints used by WooGit.
- Records every request and response for inspection.
- Supports deterministic error injection for 401/403/404/409/429/5xx and malformed responses.
- Keeps sandbox code isolated from the Android production modules.

## Intended next implementation

The sandbox service will be a standalone Kotlin/Ktor service with an in-memory or SQLite-backed fake store and a recorder API. The Android app should point its configurable API base URL at this service during integration testing.

This directory intentionally starts as documentation only; it does not change production WooGit networking or credentials.
