package com.statushd.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * StatusHD — tema Material 3 daripada replica/design/tokens.json.
 * Merah #C8102E + putih. Warna pepejal sahaja — tiada gradien,
 * tiada glassmorphism, tiada ungu/biru.
 *
 * Fon: sistem lalai buat masa ini. Inter (OFL) boleh ditambah kemudian:
 * letak fail TTF dalam res/font/ dan tukar [StatusHdFont] kepada
 * FontFamily(Font(R.font.inter_regular), ...) — lihat design.md.
 */
val Accent = Color(0xFFC8102E)
val AccentDark = Color(0xFF9E0C24)
val AccentLight = Color(0xFFFDECEF)
val ShBg = Color(0xFFFFFFFF)
val ShSurface = Color(0xFFFAF8F8)
val ShBorder = Color(0xFFE8E0E0)
val ShText = Color(0xFF1C1A1A)
val ShTextMuted = Color(0xFF6E6565)
val ShDanger = Color(0xFFB00020)
val ShSuccess = Color(0xFF1F7A4D)
val ShWarning = Color(0xFF8A5A00)

val StatusHdFont = FontFamily.Default

private val ShColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentLight,
    onPrimaryContainer = AccentDark,
    secondary = AccentDark,
    onSecondary = Color.White,
    background = ShBg,
    onBackground = ShText,
    surface = ShBg,
    onSurface = ShText,
    surfaceVariant = ShSurface,
    onSurfaceVariant = ShTextMuted,
    outline = ShBorder,
    error = ShDanger,
    onError = Color.White,
)

private val ShTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = StatusHdFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
)

private val ShShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun StatusHdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShColorScheme,
        typography = ShTypography,
        shapes = ShShapes,
        content = content,
    )
}
