package com.migux97.simon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

val SimonPurple = Color(0xFF6C4CF5)
val SimonPink = Color(0xFFFF6FA8)
val SimonYellow = Color(0xFFFFC94D)

val SimonGradient = Brush.linearGradient(listOf(SimonPurple, Color(0xFFB04CF5), SimonPink))

private val Light = lightColorScheme(
    primary = SimonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E1FF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = SimonPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E5),
    onSecondaryContainer = Color(0xFF3E0021),
    tertiary = Color(0xFFB98400),
    tertiaryContainer = Color(0xFFFFEFC7),
    background = Color(0xFFFBF7FF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFBF7FF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF4A4458),
    surfaceContainer = Color(0xFFF3EEFA),
    surfaceContainerHigh = Color(0xFFEDE7F6),
    outline = Color(0xFF7A7289),
    outlineVariant = Color(0xFFCCC4DA)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCABEFF),
    onPrimary = Color(0xFF34138F),
    primaryContainer = Color(0xFF4B2FC7),
    onPrimaryContainer = Color(0xFFE8E1FF),
    secondary = Color(0xFFFFB0CC),
    onSecondary = Color(0xFF5E1138),
    secondaryContainer = Color(0xFF7D2950),
    onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = SimonYellow,
    tertiaryContainer = Color(0xFF5C4300),
    background = Color(0xFF14111F),
    onBackground = Color(0xFFE7E0F0),
    surface = Color(0xFF14111F),
    onSurface = Color(0xFFE7E0F0),
    surfaceVariant = Color(0xFF2A2536),
    onSurfaceVariant = Color(0xFFCBC3DA),
    surfaceContainer = Color(0xFF1E1A2A),
    surfaceContainerHigh = Color(0xFF282334),
    outline = Color(0xFF948DA3),
    outlineVariant = Color(0xFF4A4458)
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun SimonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

val CaptionStyle = TextStyle(fontSize = 12.sp)
