-- StatusHD — local Room (SQLite) schema, phase 1 + phase 2 tables.
-- Clean-room design from replica/recon.md + replica/rea-analysis.md.
-- Local-first: no server, no user_id. One database on the device.
-- Dates: epoch millis (local device time is fine for local data;
-- server-side phase-2 timestamps are UTC timestamptz).
-- Money: none — the app is 100% free (Jean, 2026-11-14). No billing,
-- no paywall, no ads. Phase 2: no charges — server rate-limits AI usage
-- for cost control only.

PRAGMA foreign_keys = ON;

-- Picked gallery media -------------------------------------------------------
CREATE TABLE IF NOT EXISTS media_items (
    id          TEXT PRIMARY KEY,              -- uuid
    uri         TEXT NOT NULL,                 -- content:// URI from MediaStore
    type        TEXT NOT NULL CHECK (type IN ('video', 'photo')),
    duration_ms INTEGER NOT NULL DEFAULT 0,
    width       INTEGER NOT NULL DEFAULT 0,
    height      INTEGER NOT NULL DEFAULT 0,
    size_bytes  INTEGER NOT NULL DEFAULT 0,
    date_added  INTEGER NOT NULL               -- epoch millis
);
CREATE INDEX IF NOT EXISTS idx_media_items_date ON media_items (date_added DESC);

-- One compression run. Shape mirrors the original's CompressJob function
-- (own code): status machine pending -> running -> delivered | failed.
CREATE TABLE IF NOT EXISTS compression_jobs (
    id              TEXT PRIMARY KEY,          -- uuid
    media_id        TEXT NOT NULL REFERENCES media_items (id) ON DELETE CASCADE,
    status          TEXT NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'running', 'delivered', 'failed')),
    preset          TEXT NOT NULL DEFAULT '720p'
                    CHECK (preset IN ('720p', '1080p', '4k')),
    aspect          TEXT NOT NULL DEFAULT 'original'
                    CHECK (aspect IN ('original', '9:16')),
    quality_fhd     INTEGER NOT NULL DEFAULT 0,  -- 1 = FHD quality flag
    meta_width      INTEGER NOT NULL DEFAULT 0,
    meta_height     INTEGER NOT NULL DEFAULT 0,
    meta_rotation   INTEGER NOT NULL DEFAULT 0,
    meta_hdr        INTEGER NOT NULL DEFAULT 0,  -- 1 = HDR source
    clip_length_ms  INTEGER NOT NULL DEFAULT 0,
    duration_ms     INTEGER NOT NULL DEFAULT 0,
    progress        INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    output_uri      TEXT,
    size_before     INTEGER NOT NULL DEFAULT 0,
    size_after      INTEGER NOT NULL DEFAULT 0,
    caption         TEXT,
    sender_number   TEXT,                        -- own WhatsApp number (opt-in delivery)
    whatsapp        TEXT,                        -- WhatsApp variant used
    seq             INTEGER NOT NULL DEFAULT 0,  -- 1 = part of a sequential split
    error           TEXT,
    created_at      INTEGER NOT NULL               -- epoch millis
);
CREATE INDEX IF NOT EXISTS idx_jobs_media   ON compression_jobs (media_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status  ON compression_jobs (status, created_at DESC);
-- One running job per media item: a second tap re-attaches instead of duplicating.
CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_one_running
    ON compression_jobs (media_id) WHERE status = 'running';

-- 30-second parts of one job (splitter) --------------------------------------
CREATE TABLE IF NOT EXISTS split_parts (
    id          TEXT PRIMARY KEY,              -- uuid
    job_id      TEXT NOT NULL REFERENCES compression_jobs (id) ON DELETE CASCADE,
    part_index  INTEGER NOT NULL,              -- 0-based
    start_ms    INTEGER NOT NULL,
    end_ms      INTEGER NOT NULL,
    output_uri  TEXT,
    status      TEXT NOT NULL DEFAULT 'pending'
                CHECK (status IN ('pending', 'running', 'delivered', 'failed')),
    UNIQUE (job_id, part_index),
    CHECK (end_ms > start_ms)
);
CREATE INDEX IF NOT EXISTS idx_parts_job ON split_parts (job_id, part_index);

-- Editor state for one job (1-1) ----------------------------------------------
CREATE TABLE IF NOT EXISTS edit_projects (
    job_id          TEXT PRIMARY KEY REFERENCES compression_jobs (id) ON DELETE CASCADE,
    trim_start_ms   INTEGER NOT NULL DEFAULT 0,
    trim_end_ms     INTEGER,                   -- NULL = to end of video
    music_uri       TEXT,
    music_title     TEXT
);

-- Text overlays placed on the video -------------------------------------------
CREATE TABLE IF NOT EXISTS text_overlays (
    id          TEXT PRIMARY KEY,              -- uuid
    job_id      TEXT NOT NULL REFERENCES compression_jobs (id) ON DELETE CASCADE,
    text        TEXT NOT NULL,
    font        TEXT NOT NULL DEFAULT 'default',
    size_sp     REAL NOT NULL DEFAULT 24,
    color       INTEGER NOT NULL DEFAULT -1,   -- ARGB white
    x           REAL NOT NULL DEFAULT 0.5,     -- 0..1 relative
    y           REAL NOT NULL DEFAULT 0.5,
    rotation    REAL NOT NULL DEFAULT 0,
    glow        INTEGER NOT NULL DEFAULT 0,
    border      INTEGER NOT NULL DEFAULT 0,
    shadow      INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_texts_job ON text_overlays (job_id);

-- Sticker instances placed on the video ---------------------------------------
CREATE TABLE IF NOT EXISTS sticker_instances (
    id          TEXT PRIMARY KEY,              -- uuid
    job_id      TEXT NOT NULL REFERENCES compression_jobs (id) ON DELETE CASCADE,
    sticker_id  TEXT NOT NULL,                 -- id in the bundled sticker pack
    x           REAL NOT NULL DEFAULT 0.5,
    y           REAL NOT NULL DEFAULT 0.5,
    scale       REAL NOT NULL DEFAULT 1.0,
    rotation    REAL NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_stickers_job ON sticker_instances (job_id);

-- AI results gallery (PHASE 2) ------------------------------------------------
CREATE TABLE IF NOT EXISTS creations (
    id          TEXT PRIMARY KEY,              -- uuid
    tool        TEXT NOT NULL,                 -- e.g. photo_restore, ai_image
    input_uri   TEXT NOT NULL,
    output_uri  TEXT NOT NULL,
    created_at  INTEGER NOT NULL               -- epoch millis
);
CREATE INDEX IF NOT EXISTS idx_creations_date ON creations (created_at DESC);
