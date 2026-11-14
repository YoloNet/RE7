# Recon map: PureStatus: ByeBye Blur Status (Android)

Scope: Core loop only — pick video/photo, compress precisely for WhatsApp, share to Status in HD. Video editor, splitter, free-use limit + Premium paywall included. AI Studio deferred to phase 2 (mapped here, not built first).
For: Jean — his own product to sell, under his own name and brand (rebrand before launch).
Date: 2026-11-13

## Sources

| # | source | URL | notes |
| --- | --- | --- | --- |
| 1 | Google Play listing | https://play.google.com/store/apps/details?id=com.damtechdesigns.purepixel | Full feature list, 4-step flow, editor features, AI assistant, 60 languages, rating 4.62 (64,674) |
| 2 | App Store listing | https://apps.apple.com/us/app/purestatus-byebye-blur-status/id6444719503 | AI Studio detail, token pricing, full IAP price list, on-device compression privacy note, v2026.13 |
| 3 | AppBrain (Android) | https://www.appbrain.com/app/purestatus-byebye-blur-status/com.damtechdesigns.purepixel | 8.8M installs, 140K in last 30 days, 62.7 MB, v2026.25 (2 Nov 2026), 28 permissions, 33 libraries, user reviews |
| 4 | Privacy policy | https://damtechdesigns.com/ios/purestatus/privacy.html | No registration, data stays on device, AdMob + Firebase Analytics, Google CMP consent, first-launch privacy gate |
| 5 | Play Data safety | (Play listing, source 1) | May share Location, App activity + 2 others; may collect Personal info; encrypted in transit; data can't be deleted |
| 6 | Help center | none found | No help center or docs site. Support = in-app AI assistant + apps@damtechdesigns.com |
| 7 | Public walkthrough videos | none found via search | No tutorial videos surfaced; store screenshots (9 on Play) are the main visual source |
| 8 | MOD APK sites | excluded on purpose | Piracy (premium-unlock builds). Not used as sources, never to be built |

Developer: DamTech Designs, Vadodara, Gujarat, India. Contact: apps@damtechdesigns.com, +91 88498 81647. Former name: PurePixel (renamed to PureStatus, changelog 2026.03). On Play since Dec 2022. Ranked #8 in Video Players & Editors (Play), #10 Top Grossing in Indonesia.

## Core loop

Pick a video or photo, compress it precisely so WhatsApp does not blur it, share it to Status in HD.

## Screens

| ID | screen | route / how to reach | purpose | key components | states seen |
| --- | --- | --- | --- | --- | --- |
| S01 | Home | launch | entry to Video / Photo / Splitter / AI Studio; premium banner | mode cards, premium banner, recent items | first-run empty, filled, paywall banner |
| S02 | Privacy consent | first launch only | must accept Privacy Policy before use (per policy text) | policy text, accept button | not accepted, accepted |
| S03 | Media picker | S01 → Video/Photo/Splitter | choose source video or photo from gallery | grid, albums, search | empty, filled, permission denied |
| S04 | Video editor | S03 → video | trim, text, stickers, music before compress | playback bar, trim timeline, text tool (12 fonts, neon glow, borders, shadows), sticker canvas (alignment guides, snap-to-center), music picker (library / own files / extract audio) | editing, preview |
| S05 | Compression options | S03/S04 → compress | quality + aspect ratio choice | quality presets (720p/1080p per reviews; 4K Speed Boost), aspect ratio (Original / 9:16) | default, premium-locked option |
| S06 | Processing | S05 → start | transcode with progress + cancel | progress bar, %, cancel, file sizes | processing, slow, error (export fail reported), frozen (reported bug) |
| S07 | Result & share | S06 → done | preview result, share to WhatsApp Status, save to gallery | before/after preview, size before/after, Share to WhatsApp button, save button | success, error |
| S08 | Photo HD | S01 → Photo | photo → HD status video | progress, preview | processing, success |
| S09 | Splitter | S01 → Splitter | long video → 30s parts | parts list, per-part status, select parts | list, processing, done, frozen on 2nd clip (reported bug) |
| S10 | Premium / paywall | free limit reached, S01 banner, Settings | plans + free-use counter | plan cards (Weekly $3.99, Monthly $6.99, Yearly $69.99; Compress Pass $1.99/wk, $4.99/mo), free uses left (3/day), Restore Subscription | browsing, limit reached, purchased |
| S11 | AI Studio hub | S01 → AI Studio | tool grid + token balance (phase 2) | tool cards, token balance chip | list, low balance |
| S12 | AI Studio tool | S11 → tool | one tool: model picker, token price shown before generate (phase 2) | model picker (fast → top quality), token price, generate button, result | input, generating, result |
| S13 | My Creations | S12 → done | gallery of AI results, post to Status in one tap (phase 2) | grid, share button | empty, filled |
| S14 | AI Support Assistant | Home / Settings | in-app help chat, handoff to human support | chat bubbles, suggested questions | chatting, handed off |
| S15 | Settings | Home | language (60, incl. Malay), about, privacy, support email, restore purchases | language picker, links | — |

Screens S11–S13 are mapped for completeness; they are phase 2, not the first slice. S02, S15 partly inferred (medium confidence) from the privacy policy and standard patterns — verify in-app.

## Flows

```
F01 Compress video to HD status (core loop)
    S01 -> S03 -> S05 -> S06 -> S07
    happy path clicks: 4 (pick, compress, share, confirm in WhatsApp)
    edge: video >30s (offer splitter), low-quality source (warn, do not upscale),
          tall 9:20/9:21 aspect (black bars — reported bug), export fails,
          free limit reached -> S10
F02 Compress photo to HD status
    S01 -> S03 -> S06 -> S07
    happy path clicks: 3
    edge: photo sent in a chat arrives as a video, not a photo
F03 Split long video into 30s parts
    S01 -> S03 -> S09 -> S06 -> S07
    happy path clicks: 4
    edge: freeze on 2nd clip (reported bug, paid user), parts used in chats arrive as 30s videos
F04 Edit video, then compress
    S01 -> S03 -> S04 -> S05 -> S06 -> S07
    happy path clicks: 6+
    edge: editing/cropping AFTER compression loses quality again (listing warns)
F05 Hit free limit, upgrade to Premium
    S06/S07 -> S10 -> purchase -> resume
    happy path clicks: 3
    edge: restore subscription, purchase fails
F06 AI Studio creation (phase 2)
    S01 -> S11 -> S12 -> S13 -> S07
    happy path clicks: 5+
    edge: out of tokens -> buy token pack
F07 Get help
    S01/S15 -> S14
    edge: handoff to human support
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| Primary button | Compress, Share to WhatsApp, Generate | default, disabled, loading | S05, S07, S12 |
| Mode card | Video, Photo, Splitter, AI Studio | default, premium badge | S01 |
| Media grid | photos, videos | empty, filled, loading | S03 |
| Video player + playback bar | — | playing, paused, scrubbing | S04, S07 |
| Trim timeline | — | dragging handles | S04 |
| Text overlay editor | 12 fonts, bold templates, neon glow, borders, shadows | editing, placed | S04 |
| Sticker canvas | stickers | dragging, snap-to-center, alignment guides | S04 |
| Music picker | library, own files, extract audio | browsing, selected | S04 |
| Progress bar + cancel | — | processing, cancelling | S06, S09 |
| Before/after preview | — | side-by-side / toggle | S07 |
| Share sheet | WhatsApp Status, save to gallery | — | S07, S13 |
| Paywall card | Weekly, Monthly, Yearly, Compress Pass | default, selected, purchased | S10 |
| Free-use counter | — | n of 3 left, 0 left | S01, S10 |
| Token balance chip | — | balance, low | S11, S12 |
| Language picker | 60 languages | selected | S15 |
| Chat (AI assistant) | — | typing, answered, handed off | S14 |
| Banner + interstitial ads | AdMob | loaded, dismissed | S01, S06, S07 |

## Inferred data model

```
MediaItem   id, uri, type (video|photo), duration_s, width, height, size_bytes, date_added
            evidence: S03 picker, S04 editor
            confidence: high
CompressionJob  id, media_id, preset (720p|1080p|4K), aspect (original|9:16),
            status (queued|processing|done|failed), progress, output_uri,
            size_before, size_after
            evidence: S05 options, S06 processing; preset names from user reviews
            confidence: medium (preset labels guessed)
SplitPart   job_id, index, start_s, end_s, output_uri, status
            evidence: S09 splitter, listing "30-second parts"
            confidence: high
EditProject media_id, trim_start, trim_end, text_overlays[], stickers[], music_track
            evidence: S04 editor listing
            confidence: medium
Entitlement premium (bool), plan, expiry, free_uses_today, free_uses_reset_date,
            compress_pass (bool)
            evidence: reviews (3 free uses/day), IAP price list (App Store)
            confidence: high
AiCredit    token_balance, monthly_grant_date
            evidence: App Store (200 free on activate, 100/month)
            confidence: high
Creation    id, tool, input_uri, output_uri, created_at
            evidence: S13 My Creations (App Store)
            confidence: medium
Settings    language, quality_default
            evidence: 60 languages in listings
            confidence: medium
```

No User/account entity: no registration — "Any user can access the app directly,
there is no any registration. All Data will remain on user device locally."
(privacy policy). Purchases go through store billing, not an account.

Relationships: MediaItem 1-n CompressionJob, CompressionJob 1-n SplitPart,
MediaItem 1-1 EditProject, Entitlement 1-1 (per device), AiCredit 1-1 (per device),
Creation n (standalone).

## Feature matrix

See `features.csv`. Must: 19, should: 8, could: 10, skip: 3.

## Out of scope (cannot or should not be cloned)

- WhatsApp itself and its compression — we pre-compress for it; its network, users and chats are not ours to rebuild.
- Licensed content — the original's brand, name, logo, copy, screenshots, festival artwork and AI model output styles stay theirs. Our clone gets its own name, palette, logo and words (replica-brand).
- AI Studio's third-party AI providers — the clone needs its own provider and keys, not theirs. Tools are mapped, providers are not copied.
- MOD APK / premium-unlock circumvention — piracy, never built.
- The original's ad mediation setup — own AdMob account instead.

## Open questions (verify in-app before build)

- Exact quality presets and their bitrates (720p/1080p/4K labels are from reviews, not the listing).
- "Send compressed video to my WhatsApp number via automated WhatsApp API" — the privacy policy mentions a WhatsApp number being used this way; unclear which screen offers it.
- Whether the free-use counter counts compressions, shares, or both ("3 free WhatsApp deliveries" vs "3x compress" in reviews).
- Reference screenshots: 8 Play Store images saved in `replica/screens/` (play-screenshot-01..08) — but they are store promo graphics (before/after comparisons, headlines), not raw UI captures. Real UI reference still needs capturing from a phone running the app before replica-design.

## Size

Screens 15, flows 7, entities 9. Hard parts: on-device transcoding with precise
WhatsApp-target bitrate; aspect-ratio handling including tall 9:20/9:21 videos
(the original's own top bug); reliable export (the original's top complaint);
30s split with re-encode; store billing + free-use metering; (phase 2) AI tools
need a provider. Size: M for the agreed core slice; L with AI Studio included.
