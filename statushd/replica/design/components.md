# StatusHD components

Source: recon components list (`replica/recon.md`), restyled to the red +
white system (`replica/design/tokens.json`). All sizes in dp, type in sp.
Material 3 base (Button, Card, TextField, Dialog, BottomSheet) with
StatusHD tokens — never raw hex, never the default purple.

Removed by the 100% free decision (2026-11-14): free-use counter, paywall
card, banner + interstitial ads. They are listed at the end as removed, so
the builder does not re-add them.

## Button

```
variants  primary (red fill), secondary (white fill, red text, 1dp red border),
          ghost (no fill, red text), danger (danger fill)
sizes     sm 36dp, md 48dp, lg 56dp; full-width on forms, wrap on dialogs
states    default, pressed (accent-dark fill), focus-visible (2dp red ring
          outside), disabled (surface fill, text-muted, 38% opacity), loading
          (spinner in on-accent, label kept for screen readers)
tokens    bg accent, text on-accent, radius md, font sm/600 (md), base/600 (lg)
a11y      real Button, 48dp min touch target, contentDescription on icon-only
used on   S01 (mode cards use large buttons), S05, S06 (cancel), S07, S15
```

## Top app bar

```
variants  default (white bg, dark title), with back arrow, with actions
sizes     56dp tall, 16dp side padding, title lg/600
states    default, scrolled (1dp bottom border appears)
tokens    bg bg, text text, border border, icons text
a11y      back = real navigation, title = heading for screen readers
used on   every screen except S01 (Home uses the wordmark bar instead)
```

## Mode card (Home entry card)

```
variants  Video, Photo, Splitter, AI Studio (phase 2, shows "Soon" chip until M3)
sizes     full-width row, 72dp tall, icon 40dp in accent-light rounded square
states    default, pressed (surface darkens to accent-light), disabled (AI Studio
          before phase 2: muted, "Soon" chip)
tokens    bg surface, border border, radius lg, icon bg accent-light, icon red accent,
          title base/600, subtitle sm text-muted
a11y      whole card is one Button (not a clickable row), 48dp+ target
used on   S01
```

## Media grid item

```
variants  video (shows duration chip), photo
sizes     3-column grid, 4dp gaps, square cells, 16dp screen padding
states    default, selected (2dp red border + red check circle), loading
          (shimmer in surface)
tokens    bg surface, radius md, duration chip: black 60% bg, white xs text
a11y      contentDescription = "Video, 24 saat" / "Foto"; selected state announced
used on   S03
```

## Segmented control (quality presets)

```
variants  720p / 1080p / 4K (4K shows "Boost" label in M3; in M0–M2 it is a
          normal option, no lock — everything is free)
sizes     48dp tall, full-width, equal segments
states    selected (red fill, white text), unselected (white fill, dark text),
          disabled never (no locked options in this app)
tokens    selected bg accent, text on-accent; unselected bg bg, border border,
          radius pill for the group, md for segments
a11y      real radio group semantics, selected announced
used on   S05
```

## Bottom sheet (compression options)

```
variants  options sheet (quality + aspect + caption), share sheet
sizes     full-width, xl top radius, drag handle 32x4dp, max 90% screen height
states    collapsed (peek), expanded, dismissed (swipe / scrim tap)
tokens    bg bg, radius xl top, handle border, scrim black 32%
a11y      focus trapped while open, scrim tap dismisses, drag handle labelled
used on   S05, S07
```

## Progress bar + cancel

```
variants  linear (compression %), circular (small, inline)
sizes     linear 6dp tall, full-width; circular 24dp
states    running (red fill on accent-light track), done (success), failed
          (danger), cancelled (muted)
tokens    track accent-light, fill accent, text sm text-muted ("47% · 12 MB / 26 MB")
a11y      progress announced as percentage, cancel = real Button "Batal"
used on   S06, S09
```

## Before/after preview

```
variants  toggle (before | after), side-by-side (wide screens only)
sizes     16:9 frame, radius lg, border border
states    loading (surface + spinner), ready, failed (error state + retry)
tokens    bg surface, border border, toggle: segmented control style
a11y      toggle is a switch with labels "Sebelum" / "Selepas"; images have
          contentDescription
used on   S07
```

## Video player + playback bar

```
variants  inline preview (S04, S07), full player (editor)
sizes     16:9 frame; bar 48dp with play/pause 40dp, scrubber, time labels xs
states    playing, paused, scrubbing (time updates live), buffering (spinner)
tokens    bar bg bg, icons text, scrubber track border, fill accent, thumb accent
a11y      play/pause announced, scrubber is a Slider with time values
used on   S04, S07
```

## Trim timeline

```
variants  single trim (start/end handles)
sizes     64dp tall strip with thumbnails, handles 24dp wide
states    idle, dragging start, dragging end, invalid (end <= start: red handles)
tokens    strip bg surface, handles accent, border border, radius sm
a11y      handles are Sliders with start/end time values in seconds
used on   S04
```

## Text overlay editor

```
variants  font picker (12 fonts), style row (neon glow, border, shadow toggles)
sizes     font chips 40dp tall; canvas = video frame
states    editing (keyboard open, red outline on active text), placed, selected
tokens    chips: unselected bg bg border border; selected bg accent-light,
          border accent, text accent
a11y      text field with label, style toggles are Switches with labels
used on   S04
```

## Sticker canvas

```
variants  sticker tray (horizontal scroll), canvas (drag / pinch / rotate)
sizes     tray items 56dp, canvas = video frame
states    idle, dragging (alignment guides appear, snap-to-center in red),
          selected (red outline + delete button)
tokens    guides accent, snap line accent, selected outline accent
a11y      stickers have contentDescription; drag also adjustable via +/- buttons
used on   S04
```

## Music picker row

```
variants  library track, own file, extract audio from video
sizes     56dp row: 40dp icon, title sm/600, subtitle xs muted, trailing action
states    default, selected (red check + accent-light bg), playing (red equalizer)
tokens    bg bg, selected bg accent-light, icon accent
a11y      row is a radio option, playing state announced
used on   S04
```

## Share sheet

```
variants  WhatsApp Status (primary red button), Save to gallery, Send to my
          WhatsApp number (opt-in, shows the saved number or "Add number")
sizes     full-width buttons 56dp, stacked, 8dp gaps
states    default, WhatsApp missing (row disabled + "WhatsApp tidak dipasang")
tokens    primary bg accent text on-accent; others bg bg border border
a11y      WhatsApp row states the app it opens; disabled reason announced
used on   S07, S13
```

## Text field

```
variants  outlined (caption, WhatsApp number), search (picker)
sizes     56dp tall, radius md, 16dp horizontal padding
states    default, focused (2dp red border), error (danger border + message),
          disabled, filled
tokens    border border-input, focused border accent, error border danger,
          label xs text-muted, text base
a11y      real TextField with label, error announced, keyboard type set
          (phone for WhatsApp number)
used on   S03 (search), S04 (caption), S07 (number), S15
```

## Dialog

```
variants  confirm, error + retry, info
sizes     max 480dp wide, 24dp padding, radius xl
states    open, dismissed
tokens    bg bg, title lg/600, body base text-muted, buttons: ghost + primary
a11y      focus trapped, Escape/back dismisses, title announced
used on   S06 (cancel confirm), errors everywhere, S15 (about)
```

## Snackbar

```
variants  info, error (danger bg), success
sizes     full-width minus 16dp margins, 48dp min, bottom above nav
states    shown (4s), dismissed (swipe / timeout)
tokens    bg text (dark), text bg; error bg danger text on-accent
a11y      announced by screen reader, action button focusable
used on   everywhere (saved, failed, copied)
```

## Empty state

```
variants  no media yet, no creations yet, no results
sizes     centred, icon 64dp in accent-light circle, title lg/600, body sm muted
states    only one (empty)
tokens    icon bg accent-light, icon accent, title text, body text-muted
a11y      text is real text, action is a real Button
used on   S01 (recent), S03, S13
```

## Error state + retry

```
variants  inline (small, in place), full (centred)
sizes     inline 56dp row; full centred like empty state
states    error with message + "Cuba lagi" primary button
tokens    icon danger, message base, button primary
a11y      error announced, retry is a real Button
used on   S06, S07, S12
```

## Language picker

```
variants  list of languages (BM first, then EN, then the rest as added)
sizes     56dp rows, radio at the end
states    selected (red radio + accent-light bg)
tokens    bg bg, selected bg accent-light, radio accent
a11y      radio group semantics, language names in their own language
used on   S15
```

## Switch

```
variants  on/off only
sizes     standard Material 3 switch (52x32dp)
states    on (red track, white thumb), off (border track, muted thumb),
          disabled
tokens    on bg accent, off bg border, thumb on-accent / bg
a11y      real Switch with label, state announced
used on   S04 (text styles), S15
```

## Chip

```
variants  filter, info ("Soon", "Percuma"), tag
sizes     32dp tall, pill radius, 12dp horizontal padding
states    selected (red fill, white text), unselected (white, border),
          info (accent-light bg, accent text, no interaction)
tokens    as stated; font xs/600
a11y      filter chips are toggle buttons with pressed state
used on   S01 (AI Studio "Soon"), S03 (filters), S12 (model picker)
```

## Removed (do not build)

```
Free-use counter      removed 2026-11-14 — 100% free, no limits
Paywall card (S10)    removed 2026-11-14 — no subscriptions, no plans
Banner + interstitial removed 2026-11-14 — no ads
Token balance chip    removed 2026-11-14 — no token wallet/charges (phase 2 free)
```
