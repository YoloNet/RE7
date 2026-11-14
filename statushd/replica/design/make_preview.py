#!/usr/bin/env python3
"""Render a 3-screen red/white preview of StatusHD (Home / Options / Result).
PIL only. Fonts: DejaVu (preview only — the real app uses Inter)."""
from PIL import Image, ImageDraw, ImageFont

RED = (200, 16, 46)
RED_DARK = (158, 12, 36)
RED_LIGHT = (253, 236, 239)
WHITE = (255, 255, 255)
SURFACE = (250, 248, 248)
BORDER = (232, 224, 224)
TEXT = (28, 26, 26)
MUTED = (110, 101, 101)
DARK_PREVIEW = (90, 10, 22)

F_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
F_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def font(size, bold=False):
    return ImageFont.truetype(F_B if bold else F_R, size)


def wrap(d, text, fnt, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if d.textlength(t, font=fnt) <= max_w:
            cur = t
        else:
            lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def rrect(d, box, radius, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def phone_frame(d, x, y, w, h):
    rrect(d, [x, y, x + w, y + h], 36, fill=WHITE, outline=BORDER, width=2)
    # status bar
    d.text((x + 22, y + 14), "13:11", font=font(13, True), fill=TEXT)
    d.text((x + w - 62, y + 14), "100%", font=font(13), fill=MUTED)


def wordmark(d, x, y):
    rrect(d, [x, y, x + 34, y + 34], 9, fill=RED)
    d.polygon([(x + 13, y + 9), (x + 13, y + 25), (x + 25, y + 17)], fill=WHITE)
    d.text((x + 44, y + 4), "Status", font=font(22, True), fill=TEXT)
    w = d.textlength("Status", font=font(22, True))
    d.text((x + 44 + w + 2, y + 4), "HD", font=font(22, True), fill=RED)


def primary_btn(d, x, y, w, label, h=52):
    rrect(d, [x, y, x + w, y + h], 12, fill=RED)
    tw = d.textlength(label, font=font(16, True))
    d.text((x + (w - tw) / 2, y + 15), label, font=font(16, True), fill=WHITE)


def outline_btn(d, x, y, w, label, h=52):
    rrect(d, [x, y, x + w, y + h], 12, fill=WHITE, outline=BORDER, width=2)
    tw = d.textlength(label, font=font(16, True))
    d.text((x + (w - tw) / 2, y + 15), label, font=font(16, True), fill=TEXT)


def mode_card(d, x, y, w, icon, title, sub, chip=None):
    rrect(d, [x, y, x + w, y + 72], 16, fill=SURFACE, outline=BORDER, width=1)
    rrect(d, [x + 14, y + 16, x + 54, y + 56], 10, fill=RED_LIGHT)
    if icon == "play":
        d.polygon([(x + 27, y + 26), (x + 27, y + 46), (x + 43, y + 36)], fill=RED)
    elif icon == "photo":
        d.ellipse([x + 24, y + 25, x + 30, y + 31], fill=RED)
        d.polygon([(x + 22, y + 48), (x + 31, y + 34), (x + 38, y + 44), (x + 43, y + 38), (x + 48, y + 48)], fill=RED)
    elif icon == "split":
        d.rectangle([x + 24, y + 24, x + 44, y + 33], fill=RED)
        d.rectangle([x + 24, y + 39, x + 44, y + 48], fill=RED)
        d.line([x + 22, y + 36, x + 46, y + 36], fill=RED, width=2)
    else:  # sparkle
        cx, cy = x + 34, y + 36
        d.polygon([(cx, cy - 13), (cx + 4, cy - 4), (cx + 13, cy), (cx + 4, cy + 4),
                   (cx, cy + 13), (cx - 4, cy + 4), (cx - 13, cy), (cx - 4, cy - 4)], fill=RED)
    d.text((x + 66, y + 14), title, font=font(16, True), fill=TEXT)
    d.text((x + 66, y + 38), sub, font=font(13), fill=MUTED)
    if chip:
        cw = d.textlength(chip, font=font(11, True)) + 18
        rrect(d, [x + w - cw - 14, y + 22, x + w - 14, y + 48], 13, fill=RED_LIGHT)
        d.text((x + w - cw - 14 + 9, y + 28), chip, font=font(11, True), fill=RED)


def appbar(d, x, y, w, title):
    d.text((x + 16, y), "‹", font=font(26, True), fill=TEXT)
    d.text((x + 44, y + 3), title, font=font(19, True), fill=TEXT)


W, H = 1200, 900
img = Image.new("RGB", (W, H), WHITE)
d = ImageDraw.Draw(img)
PW, PH = 360, 780
xs = [30, 420, 810]
labels = ["S01 · Home", "S05 · Pilihan mampatan", "S07 · Hasil & kongsi"]

# ---- Screen 1: Home ----
x, y = xs[0], 40
phone_frame(d, x, y, PW, PH)
wordmark(d, x + 16, y + 48)
d.text((x + PW - 52, y + 52), "⚙", font=font(22), fill=TEXT)
hero = wrap(d, "Mampatkan video untuk Status WhatsApp — HD, tanpa kabur.", font(26, True), PW - 32)
yy = y + 108
for ln in hero:
    d.text((x + 16, yy), ln, font=font(26, True), fill=TEXT)
    yy += 34
yy += 14
for icon, t, s, chip in [("play", "Video", "Mampat video ke HD", None),
                         ("photo", "Foto", "Foto jadi video HD", None),
                         ("split", "Pemotong", "Video panjang → 30 saat", None),
                         ("sparkle", "AI Studio", "Alat AI & pelekat", "Akan datang")]:
    mode_card(d, x + 16, yy, PW - 32, icon, t, s, chip)
    yy += 84
d.text((x + 16, yy + 8), "100% percuma — tiada had, tiada iklan.",
       font=font(13), fill=MUTED)

# ---- Screen 2: Options ----
x = xs[1]
phone_frame(d, x, y, PW, PH)
appbar(d, x, y + 48, PW, "Pilihan mampatan")
yy = y + 112
d.text((x + 16, yy), "Kualiti", font=font(14, True), fill=TEXT)
yy += 30
# segmented control
seg_w = (PW - 32) // 3
for i, lab in enumerate(["720p", "1080p", "4K"]):
    sx = x + 16 + i * seg_w
    if i == 1:
        rrect(d, [sx, yy, sx + seg_w - 4, yy + 48], 12, fill=RED)
        d.text((sx + (seg_w - 4) / 2 - d.textlength(lab, font=font(15, True)) / 2, yy + 14),
               lab, font=font(15, True), fill=WHITE)
    else:
        rrect(d, [sx, yy, sx + seg_w - 4, yy + 48], 12, fill=WHITE, outline=BORDER, width=1)
        d.text((sx + (seg_w - 4) / 2 - d.textlength(lab, font=font(15, True)) / 2, yy + 14),
               lab, font=font(15, True), fill=TEXT)
yy += 72
d.text((x + 16, yy), "Nisbah", font=font(14, True), fill=TEXT)
yy += 30
for lab, sub, sel in [("Asal", "Video tinggi dipadankan dengan latar kabur", True),
                      ("9:16 (Status)", "Penuh skrin telefon", False)]:
    rrect(d, [x + 16, yy, x + PW - 16, yy + 64], 12, fill=SURFACE if sel else WHITE,
          outline=RED if sel else BORDER, width=2 if sel else 1)
    d.ellipse([x + 32, yy + 22, x + 52, yy + 42], outline=RED, width=2)
    if sel:
        d.ellipse([x + 37, yy + 27, x + 47, yy + 37], fill=RED)
    d.text((x + 64, yy + 10), lab, font=font(15, True), fill=TEXT)
    d.text((x + 64, yy + 34), sub, font=font(12), fill=MUTED)
    yy += 76
yy += 6
d.text((x + 16, yy), "Kapsyen (pilihan)", font=font(14, True), fill=TEXT)
yy += 28
rrect(d, [x + 16, yy, x + PW - 16, yy + 52], 12, fill=WHITE, outline=BORDER, width=1)
d.text((x + 30, yy + 16), "Tulis kapsyen…", font=font(15), fill=MUTED)
yy += 72
d.text((x + 16, yy), "Anggaran: 26 MB → 8 MB", font=font(14), fill=MUTED)
yy += 44
primary_btn(d, x + 16, yy, PW - 32, "Mula mampatkan", 56)

# ---- Screen 3: Result ----
x = xs[2]
phone_frame(d, x, y, PW, PH)
appbar(d, x, y + 48, PW, "Siap")
yy = y + 112
# 16:9 preview
rrect(d, [x + 16, yy, x + PW - 16, yy + 184], 16, fill=DARK_PREVIEW)
d.polygon([(x + 160, yy + 66), (x + 160, yy + 118), (x + 204, yy + 92)], fill=WHITE)
rrect(d, [x + 28, yy + 14, x + 96, yy + 40], 8, fill=RED)
d.text((x + 38, yy + 19), "1080p", font=font(13, True), fill=WHITE)
yy += 200
# before/after toggle
rrect(d, [x + 16, yy, x + PW - 16, yy + 44], 22, fill=SURFACE, outline=BORDER, width=1)
rrect(d, [x + PW // 2, yy, x + PW - 16, yy + 44], 22, fill=RED)
d.text((x + 62, yy + 12), "Sebelum", font=font(14, True), fill=MUTED)
d.text((x + PW // 2 + 52, yy + 12), "Selepas", font=font(14, True), fill=WHITE)
yy += 60
d.text((x + 16, yy), "26 MB → 8 MB · 1080p · 24 saat", font=font(14), fill=MUTED)
yy += 40
primary_btn(d, x + 16, yy, PW - 32, "Kongsi ke Status WhatsApp", 56)
yy += 68
outline_btn(d, x + 16, yy, PW - 32, "Simpan ke galeri", 56)
yy += 68
outline_btn(d, x + 16, yy, PW - 32, "Hantar ke nombor WhatsApp saya", 56)

for x, lab in zip(xs, labels):
    d.text((x + 110, y + PH + 16), lab, font=font(14, True), fill=TEXT)

img.save("/home/hatch/workspace/purestatus-clone/replica/design/preview.png")
print("saved preview.png", img.size)
