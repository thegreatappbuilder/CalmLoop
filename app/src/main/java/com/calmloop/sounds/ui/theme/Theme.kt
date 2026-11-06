package com.calmloop.sounds.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Night = Color(0xFF0B1026)
val NightMid = Color(0xFF16163D)
val Dusk = Color(0xFF2A1B4F)
val Moon = Color(0xFFF3EEFF)
val Sky = Color(0xFF8AB4FF)
val Lavender = Color(0xFFB59CFF)
val Mint = Color(0xFF7FE3D3)
val CardIdle = Color(0x14FFFFFF)
val TextSoft = Color(0xFFB9BDDA)

val BackgroundBrush = Brush.verticalGradient(listOf(Night, NightMid, Dusk))
val ActiveBrush = Brush.linearGradient(listOf(Color(0x668AB4FF), Color(0x55B59CFF)))

private val colors = darkColorScheme(
    primary = Sky,
    onPrimary = Night,
    secondary = Lavender,
    onSecondary = Night,
    tertiary = Mint,
    background = Night,
    onBackground = Moon,
    surface = Color(0xFF1A1C42),
    onSurface = Moon,
    surfaceVariant = Color(0xFF252857),
    onSurfaceVariant = TextSoft,
    surfaceContainerHigh = Color(0xFF22244F),
    outline = Color(0x33FFFFFF),
)

private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CalmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
