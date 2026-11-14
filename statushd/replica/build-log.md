# StatusHD — build log

One line per screen: ID, date, done or partial, what is missing,
what was harder than expected. (Per the replica-build skill.)

## 2026-11-14 — v1.0.0 (M0 vertical slice + M1 must-haves)

- **S01 Home — done.** Mode cards (Video / Foto / Pemotong / AI Studio),
  recent jobs strip, footer. AI Studio card is a disabled "Akan datang"
  placeholder — the tools themselves are phase 2.
- **S02 Privacy notice — done (partial vs original).** Short notice on
  first launch, not a blocking gate: there is no UMP/ads SDK in a 100%
  free app, so there is nothing to consent to. Full policy text in
  Settings. Original required acceptance to use the app.
- **S03 Picker — done.** MediaStore grid (videos + photos), search by
  name, All/Video/Photo filters, duration badges, permission states
  (granted / denied / empty). Thumbnails via MediaMetadataRetriever
  (video frame) / BitmapFactory (photo) with an in-memory cache.
- **S04 Editor — NOT built (M2).** Trim, text overlays, stickers, music
  are the next milestone. Tables + DAOs for them already exist in Room.
- **S05 Options — done.** Quality segmented control (720p/1080p/4K),
  aspect radios (Original / 9:16), caption, rough size estimate
  (labelled "Anggaran" — real size depends on content).
- **S06 Processing — done.** Foreground service (mediaProcessing),
  progress bar + percent + sizes, cancel with confirmation.
  Cancel marks the job failed("cancelled") — the screen then offers
  retry or back.
- **S07 Result — done.** VideoView preview (auto-play loop) with
  Before/After toggle, sizes line, share to WhatsApp (FileProvider),
  save to gallery (Movies/StatusHD), send-to-my-number (wa.me chat).
  Photo jobs show the original photo as "Before".
- **S08 Photo HD — done.** Photo becomes a 5-second silent HD video
  (loop + anullsrc audio) through the same S06/S07 flow.
- **S09 Splitter — done.** 30-second parts, per-part status + share,
  save-all to gallery. "Share all" shares part 1 and asks the user
  to repeat in order — WhatsApp has no multi-video Status share.
- **S10 Paywall — deleted by design.** 100% free: no paywall, no
  subscriptions, no free-use limit, no ads, no watermark.
- **S11–S13 AI Studio / My Creations — NOT built (phase 2).** Needs
  a server (Supabase Edge Functions) + Jean's own provider keys.
  Tables for creations already exist in Room.
- **S14 Support — done (FAQ-first).** 4 FAQs as an accordion + email
  button. The original's in-app AI chat assistant is NOT built;
  FAQ-first was the architecture's M2 choice.
- **S15 Settings — done.** Language BM/EN (recreate on change),
  default quality, privacy policy + re-show notice, about.

## What was harder than expected

- **Maven Central is not directly reachable from this VM.**
  repo.maven.apache.org and repo1.maven.org return 404/empty through
  the proxy. Dependency versions were verified via Google's Maven
  mirror (maven-central.storage-download.googleapis.com) — but that
  mirror is partial: it has ffmpeg-kit-full (6.0-2.LTS verified,
  AAR 200 OK) yet 404s on androidx artifacts (navigation-compose,
  room). Consequence: the build uses **only** dependencies already
  proven by kuv2 on Jean's PC, plus ffmpeg-kit-full. Navigation is
  state-based (like kuv2), video preview is VideoView, locale is
  applied manually — no navigation-compose, no ExoPlayer, no appcompat.
- **FFmpegKit's AAR is 65 MB.** The APK will be ~70 MB+, same as the
  original (62.7 MB). Accepted — the original ships the same stack.
- **Gradle still cannot run in this VM** (loopback blocked, same as
  KebunKu/PokerKu). The app is **not verified by a real build**.
  What IS verified: pure-Kotlin FFmpeg command builder, 35/35 checks
  run on the JVM (`bash tools/run-tests.sh`); string parity BM/EN
  (106 keys); every dependency version either built on Jean's PC
  (kuv2 baseline) or verified on Maven Central (ffmpeg-kit-full
  6.0-2.LTS). First real build is on Jean's PC.
- **WhatsApp-target bitrate presets are estimates** (CRF 24/22/20 +
  maxrate caps). The original's exact presets are unknown (obfuscated
  code). Measure the real output on a phone; tune if WhatsApp still
  re-compresses visibly.
- **"Send to my number" is a v1 approximation.** WhatsApp has no
  intent that pre-selects a chat with a file attached. v1 opens a
  wa.me chat with your own number; the video is then attached from
  the share sheet. The original's exact delivery flow is unknown
  (obfuscated); this is the honest closest equivalent.

## Next (only when Jean says so)

1. **M2 editor** — trim timeline, text overlays, sticker canvas,
   music picker (+ extract audio). Tables exist; UI + FFmpeg
   drawtext/overlay filters are the work.
2. **M2 AI support chat** (or keep FAQ-first — Jean's call).
3. **M3 AI Studio** — Supabase Edge Functions + Storage, Jean's keys.
4. Real-phone verification: first Gradle build, one compress, one
   split, one share to Status; measure WhatsApp re-compression.
