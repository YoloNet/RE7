# StatusHD

StatusHD is Jean's own HD WhatsApp Status compressor app — a clean-room
rebuild of the *functions* of PureStatus (DamTech Designs), with own code,
own name, own brand, own assets. Nothing here is copied from the original
app: no code, no assets, no copy, no endpoints, no accounts.

Working title: **StatusHD** · applicationId (working): `com.statushd.app`

## What is in this folder

| path | what |
| --- | --- |
| `replica/recon.md` | Recon map of PureStatus (public sources, 2026-11-13): 15 screens, 7 flows, 9 entities, 40 features |
| `replica/features.csv` | Feature matrix: 19 must, 8 should, 10 could, 3 skip |
| `replica/rea-analysis.md` | Static analysis of the real PureStatus APK v2026.25 (2026-11-14): FFmpegKit engine, 51 own classes, RevenueCat billing, AI catalog (22 tools, 176 festivals), privacy findings |
| `replica/architecture.md` | Architecture: native Android (Kotlin + Compose) + FFmpegKit + Room, local-first, 100% free, Supabase only in phase 2 for AI Studio |
| `replica/schema.sql` | Local Room (SQLite) schema: 9 tables |

## Scope

- **Phase 1 (core):** pick video/photo → compress precisely for WhatsApp →
  share to HD Status. Editor, 30 s splitter. **100% free (Jean, 2026-11-14):**
  no paywall, no subscriptions, no limits, no ads — every feature free.
- **Phase 2 (AI Studio):** AI tools + token wallet + My Creations, festival
  catalog, server-side AI via own provider keys.

## Build order

`replica-architect` (done) → `replica-design` → `replica-build` → `replica-test`
→ `replica-diff` → `replica-entrepreneur` → `replica-brand` → `replica-launch`
→ `replica-deploy`.

## Rules

- Clean-room only. Never copy the original's code, assets, brand, or copy.
- Never build a MOD APK / premium unlock of the original — piracy.
- Never use the original's private endpoints or accounts.
