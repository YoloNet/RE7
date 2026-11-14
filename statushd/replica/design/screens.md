# StatusHD screens

Layout rules for every screen: white background (`bg`), 16dp side padding,
top app bar 56dp (except S01), content max 480dp wide centred on tablets,
bottom content padding 24dp + navigation bar insets. Red is used only for
primary actions, active states and the brand mark — never as a page
background, never as a gradient. Screens use recon S-IDs.

## S01 Home

```
Wordmark bar (56dp): red rounded-square mark + "StatusHD" (Status in text,
  HD in accent), Settings icon right.
Hero (no image): "Mampatkan video untuk Status WhatsApp — HD, tanpa kabur."
  display 28/700, 2 lines max, 24dp top gap.
Mode cards (vertical, 12dp gaps): Video, Photo, Splitter, AI Studio.
  AI Studio card shows "Akan datang" chip until M3.
Recent (below, if any): "Terkini" label + horizontal media strip (96dp thumbs).
  Empty: no recent section at all (no empty state on Home).
Bottom: "100% percuma — tiada had, tiada iklan." xs text-muted, centred.
Nav: mode cards navigate; Settings icon → S15; no bottom nav.
States: first-run (no recent), filled, AI Studio disabled ("Akan datang").
```

## S02 Privacy notice (first launch only)

```
Centred card, not a blocking gate: app mark, title "Privasi anda",
  3 short lines with red check icons:
  "Tiada akaun — data kekal dalam telefon anda."
  "Mampatan dibuat dalam telefon — video tidak dihantar ke server."
  "Tiada iklan, tiada penjejakan."
Primary button "Faham" (dismisses, remembered in DataStore).
Link "Baca dasar privasi" (ghost) → in-app text page.
States: shown once; never again unless "Tunjuk semula" in Settings.
```

## S03 Media picker

```
Top bar: back + title ("Pilih video" / "Pilih foto") + search icon.
Filter chips: Semua / Video / Foto (only the relevant one for the entry mode).
Grid: 3 columns, 4dp gaps, square cells; video cells show duration chip.
Permission denied: empty state + "Benarkan akses media" primary button.
Selection: single tap picks immediately (no multi-select in v1).
States: loading (shimmer), filled, empty ("Tiada video dijumpai"), denied.
```

## S04 Video editor

```
Top bar: back + "Edit video" + "Selesai" (primary, text button).
Player: 16:9 frame, playback bar below.
Trim timeline: 64dp strip with handles (see components.md).
Tool tabs (horizontal, below timeline): Trim | Teks | Pelekat | Muzik.
  Each tab opens its panel (font chips / sticker tray / music rows).
  Only one panel open at a time; panel is a bottom sheet on small screens.
Bottom: primary button "Mampatkan" (lg, full-width) → S05.
States: editing, preview playing, panel open.
```

## S05 Compression options (bottom sheet from S04, or screen from S03)

```
Sheet: drag handle, title "Pilihan mampatan".
Quality: segmented control 720p / 1080p / 4K.
Aspect: two radio rows — "Asal" / "9:16 (Status)".
  Tall video (9:20/9:21) note under Asal: "Video tinggi dipadankan
  dengan latar kabur, bukan bar hitam." (architecture decision)
Caption: optional text field (sent with the status, not burned in).
Estimate: "Anggaran: 26 MB → 8 MB" (sm muted, updates with preset).
Bottom: primary button "Mula mampatkan" (lg) → S06.
States: default; estimate loading (spinner in place of the number).
```

## S06 Processing

```
Centred: circular preview thumb (96dp, radius lg), file name (sm),
  linear progress bar + "47% · 12 MB / 26 MB" (sm muted).
Cancel: ghost danger button "Batal" → confirm dialog → job cancelled,
  partial file deleted, back to S05.
Error: error state + "Cuba lagi" (primary) + "Kembali" (ghost).
  The real error message is shown (not a generic "failed").
Done: auto-advance to S07 (no separate done screen).
Background: job continues in the foreground service with a notification;
  reopening the app re-attaches to the running job.
States: running, cancelled, failed, done (transient).
```

## S07 Result & share

```
Top bar: back (to Home) + "Siap" title.
Before/after preview: 16:9 frame + toggle "Sebelum | Selepas".
Size line: "26 MB → 8 MB · 1080p · 24 saat" (sm muted).
Share (stacked, 8dp gaps):
  Primary red: "Kongsi ke Status WhatsApp".
  Secondary: "Simpan ke galeri".
  Secondary: "Hantar ke nombor WhatsApp saya" (shows saved number or
    "Tambah nombor" — opt-in; number stored locally only).
WhatsApp missing: primary button disabled + note "WhatsApp tidak dipasang".
Snackbar on save: "Disimpan ke galeri".
States: ready, saving, WhatsApp missing.
```

## S08 Photo HD

```
Same shell as S06/S07 but for a photo: pick photo → progress
("Menukar foto kepada video HD…") → result screen identical to S07
with note "Foto dihantar sebagai video dalam sembang."
```

## S09 Splitter

```
Top bar: back + "Pemotong 30 saat".
Source row: thumb + file name + duration.
Parts list: rows "Bahagian 1 · 0:00–0:30" with per-part status
  (pending / running / delivered / failed) + progress for the active part.
  Each part is encoded independently (architecture rule — no shared state).
Bottom: primary button "Kongsi semua ke Status" (enabled when ≥1 part done)
  + secondary "Simpan semua ke galeri".
Error on one part: that row shows retry; other parts unaffected.
States: list, processing, done, part-failed.
```

## S11 AI Studio hub (phase 2, M3)

```
Top bar: back + "AI Studio".
Grid: 2 columns of tool cards (icon in accent-light square, title sm/600,
  subtitle xs muted, "Percuma" chip — no token prices, no wallet).
Sections: Foto / Pelekat / Video / Cerita (same 4 sections as the
  original's catalog, own tools inside).
Tool card tap → S12. No balance chip (no tokens in this app).
States: list; tools load from the bundled catalog JSON.
```

## S12 AI Studio tool (phase 2, M3)

```
Top bar: back + tool name.
Model picker: chips "Pantas / Seimbang / Kualiti terbaik".
Input: photo picker or prompt field (per tool).
Generate: primary button "Jana" (free — no price shown).
Generating: progress + "Biasanya 10–30 saat" + cancel.
Result: preview + primary "Kongsi ke Status" + secondary "Simpan".
Out of daily usage (server cost cap): polite notice
  "Had harian dicapai — cuba lagi esok." (cost control, not a paywall).
States: input, generating, result, capped.
```

## S13 My Creations (phase 2, M3)

```
Top bar: back + "Hasil saya".
Grid: 2 columns, square thumbs, newest first.
Tap: preview + "Kongsi ke Status" + "Simpan" + delete (danger ghost).
Empty: empty state + "Cuba AI Studio" primary button → S11.
States: filled, empty.
```

## S14 AI Support Assistant (M2)

```
Top bar: back + "Bantuan".
Phase 1: FAQ list (accordion rows) + "E-mel sokongan" primary button
  (opens mail app to the support address — no in-app chat server).
Phase 2 (if Jean wants): in-app chat via own support API.
States: list, FAQ open, mail app opened.
```

## S15 Settings

```
Top bar: back + "Tetapan".
Sections (list rows, 56dp):
  Bahasa — "Bahasa Melayu" → language picker (BM first, then EN).
  Kualiti lalai — "720p" → radio list.
  Privasi — "Dasar privasi" → text page; "Tunjuk notis privasi semula".
  Tentang — "StatusHD 1.0.0" → about dialog (version, no account, no ads).
No account row, no restore purchases, no subscription row (100% free).
States: default; language change applies instantly.
```

## S10 — removed

The original's Premium/paywall screen does not exist in StatusHD
(100% free decision, 2026-11-14). No screen, no route, no component.
