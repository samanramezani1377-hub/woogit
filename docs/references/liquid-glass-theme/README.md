# Liquid Glass Theme — Visual Reference

**For AI agents:** this folder is a **visual/token reference only**, not source code to import.
`preview.html` is a static, dependency-free HTML/CSS mockup of a WooGit home screen. It exists so
any AI agent (Claude, GPT, Gemini, Copilot, etc.) working on WooGit's Jetpack Compose UI can open
one file, see the intended look, and read off exact color/radius/blur/type values instead of
guessing or re-deriving a "Liquid Glass" look from the name alone.

Do not copy HTML/CSS into the Android app. Translate the tokens below into Compose
(`Color`, `Dp`, `Shape`, `TextStyle`) and into the existing `Glass*` primitives required by
[`docs/V1_DESIGN_SPEC.md`](../../V1_DESIGN_SPEC.md) (`GlassCard`, `GlassButton`, `GlassChip`, etc.).
This file is downstream of that spec, not a replacement for it — if the two ever disagree,
`V1_DESIGN_SPEC.md` and `UI_UX_PRO_MAX.md` win.

## How to look at it

Open `preview.html` directly in any browser (no build step, no server needed). It renders a
390×844 phone frame with a WooGit home feed: a pulsing "new order" card, a store-stats card, a
product card, a sync-status card, and a gradient CTA, plus a glass bottom nav. Persian/RTL sample
content is used throughout since WooGit V1 is Persian-first.

## Design tokens

All values are also defined as CSS custom properties at the top of `preview.html`
(`:root { ... }`) — read them there for the authoritative list. Summary:

### Color

| Token | Hex / value | Use |
|---|---|---|
| `--bg-base` | `#EFF1F7` | Base background under the color blobs |
| `--blob-mint` | `#BEEFDC` | Background accent blob |
| `--blob-peach` | `#FFE1C2` | Background accent blob |
| `--blob-lavender` | `#D8CEFF` | Background accent blob |
| `--blob-sky` | `#C6E6FF` | Background accent blob |
| `--glass-fill` | `rgba(255,255,255,0.52)` | Default glass card fill |
| `--glass-fill-strong` | `rgba(255,255,255,0.72)` | Bottom nav fill (needs more contrast/legibility) |
| `--glass-border` | `rgba(255,255,255,0.65)` | 1px hairline border on every glass surface |
| `--ink-900` / `--ink-600` / `--ink-400` | `#1B1F2A` / `#5B6272` / `#8A90A0` | Primary / secondary / tertiary text |
| `--accent-1` → `--accent-2` | `#6C5CE7` → `#E84393` | Brand gradient (logo mark, CTA buttons) |
| `--urgent` | `#FF6B4A` | New-order urgency (pillar 1 of the product — see below) |
| `--live` | `#22C55E` | Live/synced/success state |
| `--badge` | `#EF4444` | Notification count badges |

### Surfaces

- Glass card: `background: var(--glass-fill)`, `border: 1px solid var(--glass-border)`,
  `backdrop-filter: blur(24px) saturate(160%)`, `border-radius: 26px`
  (Compose: `Modifier.background(Color.White.copy(alpha=0.52f), RoundedCornerShape(26.dp))`
  + a blurred background layer behind it, since Compose has no native `backdrop-filter` —
  use `Modifier.blur()` on a duplicated background layer or `haze`/`androidx.compose.ui.graphics.BlurEffect`).
- Radii: `--radius-lg: 26px` (cards), `--radius-md: 18px`, `--radius-sm: 12px` (chips/icons).
- Every card has a subtle diagonal "sheen" (`::before`, white gradient top-left → transparent) —
  this is what reads as "glass" rather than plain translucency. Reproduce with a
  `Brush.linearGradient` overlay, not a flat white tint.

### Typography

- Persian/UI text: **Vazirmatn** (weights 400–900).
- Numbers, SKUs, timestamps, metadata: **JetBrains Mono** — deliberately switched out for
  monospace wherever a value is a number or code, so data scans cleanly against prose.

### Signature element — do not drop this when porting

The "new order" card has an animated **pulsing ring** (`.card--urgent .ring`, 2.4s ease-out,
expanding + fading `box-shadow`) plus a small solid **live dot**. This is intentional, not
decoration: WooGit's #1 non-negotiable pillar is near-real-time new-order notification (see
`docs/NOTIFICATION_SPEC.md` / `docs/V1_NOTIFICATION_SPEC.md`). The pulse is the visual expression
of "this just happened and is live," and should appear on any surface representing a fresh,
unacknowledged order — not on generic list items. Don't reuse the pulse for non-urgent content or
it stops meaning anything.

### Bottom navigation

Glass surface (`--glass-fill-strong`, more opaque than cards for legibility), 3 items
(خانه / ساخت / اعلان‌ها), active item gets a soft accent-tinted icon background, notification
badge is a solid `--badge` circle top-left of the icon (RTL: visually left side of a right-aligned
icon — verify against actual RTL mirroring rules in Compose, this is a CSS mockup and may need
`LayoutDirection`-aware placement).

## Accessibility notes carried over from `V1_DESIGN_SPEC.md`

This mockup is a still/animated visual reference, not an accessibility-verified surface. When
implementing in Compose, the existing rules still apply and are not waived by this file: 48dp
minimum touch targets, no color-only status signaling, content descriptions on icons, contrast
must hold over the glass/blur backgrounds, and RTL must be verified with real Persian text and
mixed-direction values (SKUs, prices, order numbers).

## Provenance

Generated as a design-direction reference from a rough concept screenshot; refined into a
token-driven mockup. Treat it as a snapshot of current direction, not a frozen spec — if
`V1_DESIGN_SPEC.md` or `UI_UX_PRO_MAX.md` evolve the Liquid Glass system, update or regenerate
this file to match rather than letting it drift out of sync.
