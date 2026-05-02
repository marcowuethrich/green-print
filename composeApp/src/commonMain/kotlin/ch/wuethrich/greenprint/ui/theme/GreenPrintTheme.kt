package ch.wuethrich.greenprint.ui.theme

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

// Modern, sophisticated green palette
private val EmeraldDark = Color(0xFF064E3B)
private val EmeraldPrimary = Color(0xFF10B981)
private val EmeraldLight = Color(0xFFD1FAE5)
private val SageSecondary = Color(0xFF6B7280)
private val SoftWhite = Color(0xFFF9FAFB)
private val PureWhite = Color(0xFFFFFFFF)
private val ErrorRed = Color(0xFFEF4444)

private val GreenColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = SageSecondary,
    onSecondary = PureWhite,
    background = SoftWhite,
    surface = PureWhite,
    onBackground = EmeraldDark,
    onSurface = EmeraldDark,
    error = ErrorRed,
    onError = PureWhite,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GreenPrintTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GreenColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

object DesignSystem {
    val BackgroundGradient = listOf(Color(0xFFECFDF5), Color(0xFFF9FAFB))
    val CardElevation = 0.dp // Prefer soft borders and subtle fills over shadows for a modern look
    val CardBorder = 1.dp
    val SoftShadow = Color(0x0A000000)
}
