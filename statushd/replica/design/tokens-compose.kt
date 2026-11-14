// StatusHD design tokens — Compose theme.
// Source of truth: replica/design/tokens.json (red + white, Jean 2026-11-14).
// Contrast: 14 pairs, 0 AA failures (contrast.py).
// Drop into the Android project at: app/src/main/java/com/statushd/app/ui/theme/
// during replica-build. Font: Inter (OFL) in res/font, fallback system sans.
package com.statushd.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object StatusHdColors {
    val Accent = Color(0xFFC8102E)      // primary red — buttons, active states, brand
    val AccentDark = Color(0xFF9E0C24)  // pressed / dark red
    val AccentLight = Color(0xFFFDECEF) // tint — selected chips, highlights
    val Bg = Color(0xFFFFFFFF)          // app background — white
    val Surface = Color(0xFFFAF8F8)     // card background — barely warm white
    val Border = Color(0xFFE8E0E0)      // card / divider border
    val BorderInput = Color(0xFF8A7F7F) // text-field border (3.87:1 vs white, UI pass)
    val Text = Color(0xFF1C1A1A)        // primary text — 17.33:1 on white (AAA)
    val TextMuted = Color(0xFF6E6565)   // secondary text — 5.66:1 on white (AA)
    val Danger = Color(0xFFB00020)      // errors, destructive actions
    val Success = Color(0xFF1F7A4D)     // done states
    val Warning = Color(0xFF8A5A00)     // warnings
    val OnAccent = Color(0xFFFFFFFF)    // text on red — 5.88:1 (AA)
}

object StatusHdType {
    // Inter, fallback system sans. Sizes in sp, line heights in sp.
    val Display = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp)
    val Xl = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
    val Lg = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)
    val Base = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val Sm = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val Xs = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    // Buttons and labels use Base/Sm with SemiBold weight (see components.md).
}

object StatusHdShapes {
    val Sm = 8.dp    // chips, small controls
    val Md = 12.dp   // buttons, inputs
    val Lg = 16.dp   // cards
    val Xl = 24.dp   // bottom sheets, large cards
    val Pill = 999.dp // pills, avatars
}

object StatusHdSpace {
    // 4dp base. Use these constants, not ad-hoc numbers.
    val S0 = 0.dp
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp   // screen side padding, default gap
    val S5 = 24.dp
    val S6 = 32.dp
    val S7 = 48.dp
    val S8 = 64.dp
}

// Rules (Jean's taste, enforced here, not per screen):
// - Solid colours only. No gradients, no glassmorphism, no purple/blue.
// - White background everywhere; red is the only chromatic colour (plus
//   green/amber/red for success/warning/danger states).
// - Cards: white/surface fill + 1dp border + 1dp elevation. No heavy shadows.
// - Icons: Material Symbols Rounded (open, Apache 2.0). No emoji in UI.
