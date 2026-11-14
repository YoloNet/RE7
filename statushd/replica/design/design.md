# Design: StatusHD

Theme: **red + white** (Jean, 2026-11-14). Solid colours only — no
gradients, no glassmorphism, no purple/blue. Professional, direct, fast to
read. This document is the index; the working files are in this folder.

## Files

| file | what |
| --- | --- |
| `tokens.json` | Source of truth: colour roles, type scale, spacing, radius, shadow, motion + contrast `pairs` |
| `tokens-compose.kt` | The same tokens as a Compose theme file (drop into the app in replica-build) |
| `components.md` | Spec for every component (variants, sizes, states, tokens, a11y, used-on) |
| `screens.md` | Layout spec for all 14 screens (S01–S09, S11–S15; S10 removed) |
| `preview.png` | Rendered preview of Home / Options / Result in the red + white system |

## Brand

- **Name:** StatusHD (working title, folder `statushd`).
- **Wordmark:** "Status" in near-black (`text`), "HD" in red (`accent`),
  Inter Bold. Used in the Home bar and the About screen.
- **App icon (adaptive):** red background (`accent` #C8102E), white
  foreground glyph — a rounded "status frame" rectangle with a play
  triangle inside. White on red, no text in the icon, no gradient.
  Built as adaptive-icon XML in replica-build (foreground + background
  layers), not a PNG.
- **Voice:** short BM labels, verbs first ("Mampatkan", "Kongsi", "Simpan").
  No emoji in UI. No marketing superlatives in the app itself.

## Colour roles (the system, not the hex)

| role | value | job |
| --- | --- | --- |
| `accent` | #C8102E | Primary red — buttons, active states, brand. White text on it: 5.88:1 |
| `accent-dark` | #9E0C24 | Pressed red |
| `accent-light` | #FDECEF | Tint — selected chips, highlights, progress track |
| `bg` | #FFFFFF | Page background — white everywhere |
| `surface` | #FAF8F8 | Card background — barely warm white |
| `border` | #E8E0E0 | Card / divider border |
| `border-input` | #8A7F7F | Text-field border (3.87:1, UI pass) |
| `text` | #1C1A1A | Primary text — 17.33:1 on white (AAA) |
| `text-muted` | #6E6565 | Secondary text — 5.66:1 on white (AA) |
| `danger` | #B00020 | Errors, destructive actions |
| `success` | #1F7A4D | Done states |
| `warning` | #8A5A00 | Warnings |

Red is the only chromatic colour in the interface. Green/amber appear only
as status colours. Cards are white/surface + 1dp border + 1dp elevation.

## Type + spacing

- **Font:** Inter (OFL, bundled in `res/font`), fallback system sans.
  Scale 12 / 14 / 16 / 20 / 28 / 40, weights 400 / 500 / 600 / 700.
- **Spacing:** 4dp base — 4, 8, 12, 16 (screen padding + default gap),
  24, 32, 48, 64.
- **Radius:** 8 (chips) / 12 (buttons, inputs) / 16 (cards) / 24 (sheets)
  / pill.
- **Motion:** 120ms fast, 200ms standard, 300ms emphasized, ease
  `cubic-bezier(.2,.8,.2,1)`.
- **Icons:** Material Symbols Rounded (open, Apache 2.0).

## Contrast report

`python3 contrast.py tokens.json` (replica-design skill tool):
**14 pairs, 0 failing AA.** Text on white 17.33:1 (AAA); muted text 5.66:1
(AA); white on red 5.88:1 (AA); red text on white 5.88:1 (AA); input
border 3.87:1 (UI pass). Full output was verified at design time; re-run
after any palette change.

## What this design does NOT contain

- No paywall screen (S10), no free-use counter, no ads, no token wallet
  (100% free decision, 2026-11-14).
- No copied screens, copy, icons, or artwork from PureStatus — layouts are
  specified from the recon screen map in our own words, and the red/white
  system is Jean's own choice (replica-brand refines it before launch if
  he wants).
- No bottom navigation in phase 1 — Home is the hub (mode cards), matching
  the recon map.

## Next step

`/replica-build` — build the primitives from `components.md` once, in
isolation (Compose `@Preview` screenshots), then the screens from
`screens.md` in build order (M0 vertical slice first).
