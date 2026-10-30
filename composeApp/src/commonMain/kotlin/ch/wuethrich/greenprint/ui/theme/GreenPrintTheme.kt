package ch.wuethrich.greenprint.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.inter_variable
import org.jetbrains.compose.resources.Font

// ─────────────────────────────────────────────────────────────────────────────
//  GreenPrint — Spring Garden theme
//  Source of truth: docs/style_guide.md
//  Calm, modern, natural. One typeface. Soft surfaces. Quiet motion.
// ─────────────────────────────────────────────────────────────────────────────

internal object Palette {
    // Foundation
    val Background = Color(0xFFF7F9F5) // sage-tinted off-white canvas
    val Surface = Color(0xFFFFFFFF) // cards, sheets
    val SurfaceDim = Color(0xFFEDF1E8) // recessed surfaces (inputs)

    // Brand greens
    val Primary = Color(0xFF4F9461) // fresh sage
    val PrimaryDark = Color(0xFF3D7A4D) // pressed
    val PrimaryLight = Color(0xFFDDE9D8) // tinted backgrounds, primary container

    // Warm coral accent — celebrations and delight only
    val Accent = Color(0xFFF2A47A)
    val AccentLight = Color(0xFFFBE4D5)

    // Text
    val Text = Color(0xFF1F2D1F) // deep forest near-black
    val TextMuted = Color(0xFF5A6B5A) // warm gray-green
    val TextSubtle = Color(0xFF95A095) // tertiary, disabled

    // Lines
    val Outline = Color(0xFFD3DCD0) // subtle borders, dividers

    // System
    val Error = Color(0xFFD14B4B)
    val ErrorContainer = Color(0xFFFCE4E4)
    val Success = Color(0xFF4F9461) // shares the brand green
    val Warning = Color(0xFFE8B25E)
}

private val GreenColorScheme = lightColorScheme(
    primary = Palette.Primary,
    onPrimary = Palette.Surface,
    primaryContainer = Palette.PrimaryLight,
    onPrimaryContainer = Palette.PrimaryDark,
    secondary = Palette.Accent,
    onSecondary = Palette.Text,
    secondaryContainer = Palette.AccentLight,
    onSecondaryContainer = Palette.Text,
    tertiary = Palette.PrimaryDark,
    onTertiary = Palette.Surface,
    tertiaryContainer = Palette.PrimaryLight,
    onTertiaryContainer = Palette.PrimaryDark,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceDim,
    onSurfaceVariant = Palette.TextMuted,
    surfaceTint = Palette.Primary,
    inverseSurface = Palette.Text,
    inverseOnSurface = Palette.Background,
    inversePrimary = Palette.PrimaryLight,
    outline = Palette.Outline,
    outlineVariant = Palette.Outline,
    error = Palette.Error,
    onError = Palette.Surface,
    errorContainer = Palette.ErrorContainer,
    onErrorContainer = Palette.Error,
    scrim = Palette.Text,
)

// ─────────────────────────────────────────────────────────────────────────────
//  Typography — Inter, set with intent.
//  One family, hierarchy from weight + size.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun interFamily(): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            Res.font.inter_variable,
            weight,
            FontStyle.Normal,
            FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private fun FontFamily.style(weight: FontWeight, size: Int, lineHeight: Int, tracking: Float): TextStyle = TextStyle(
    fontFamily = this,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

@Composable
private fun appTypography(): Typography {
    val inter = interFamily()
    return Typography(
        // Display — page heroes (rare)
        displayLarge = inter.style(FontWeight.Bold, size = 40, lineHeight = 44, tracking = -0.6f),
        displayMedium = inter.style(FontWeight.Bold, size = 36, lineHeight = 40, tracking = -0.5f),
        displaySmall = inter.style(FontWeight.Bold, size = 30, lineHeight = 36, tracking = -0.4f),
        // Headlines
        headlineLarge = inter.style(FontWeight.SemiBold, size = 28, lineHeight = 34, tracking = -0.4f),
        headlineMedium = inter.style(FontWeight.SemiBold, size = 24, lineHeight = 30, tracking = -0.3f),
        headlineSmall = inter.style(FontWeight.SemiBold, size = 22, lineHeight = 28, tracking = -0.3f),
        // Titles
        titleLarge = inter.style(FontWeight.SemiBold, size = 20, lineHeight = 26, tracking = -0.2f),
        titleMedium = inter.style(FontWeight.SemiBold, size = 16, lineHeight = 22, tracking = -0.1f),
        titleSmall = inter.style(FontWeight.Medium, size = 14, lineHeight = 20, tracking = 0f),
        // Body
        bodyLarge = inter.style(FontWeight.Normal, size = 16, lineHeight = 26, tracking = 0f),
        bodyMedium = inter.style(FontWeight.Normal, size = 14, lineHeight = 22, tracking = 0f),
        bodySmall = inter.style(FontWeight.Normal, size = 12, lineHeight = 18, tracking = 0.1f),
        // Labels
        labelLarge = inter.style(FontWeight.Medium, size = 14, lineHeight = 20, tracking = 0.1f),
        labelMedium = inter.style(FontWeight.Medium, size = 13, lineHeight = 18, tracking = 0.1f),
        labelSmall = inter.style(FontWeight.Medium, size = 12, lineHeight = 16, tracking = 0.2f),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shapes — friendly geometry. All corners rounded, just at different scales.
// ─────────────────────────────────────────────────────────────────────────────
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), // chips, tags
    small = RoundedCornerShape(12.dp), // small buttons, compact inputs
    medium = RoundedCornerShape(16.dp), // inputs, dropdowns, alerts
    large = RoundedCornerShape(24.dp), // cards, sheets, modals
    extraLarge = RoundedCornerShape(32.dp), // hero cards, large surfaces
)

@Composable
fun GreenPrintTheme(animationsEnabled: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAnimationsEnabled provides animationsEnabled) {
        MaterialTheme(
            colorScheme = GreenColorScheme,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}

val LocalAnimationsEnabled = staticCompositionLocalOf { true }

// ─────────────────────────────────────────────────────────────────────────────
//  DesignSystem — non-Material tokens (spacing scale, motion durations, and
//  occasional palette accents that don't map cleanly to Material's color roles).
// ─────────────────────────────────────────────────────────────────────────────
object DesignSystem {

    // Background wash — kept as a near-flat Background → SurfaceDim gradient so
    // existing call sites that use Brush.verticalGradient still get a subtle
    // depth without going against the calm aesthetic.
    val BackgroundGradient = listOf(Palette.Background, Palette.SurfaceDim)

    // Card defaults — soft elevation, no manual shadows.
    val CardElevation = 1.dp
    val CardElevationHovered = 3.dp

    // Border thickness for the rare cases where elevation isn't enough.
    val CardBorder = 1.dp

    // Spacing scale (4dp base)
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 24.dp
    val Space6 = 32.dp
    val Space7 = 48.dp
    val Space8 = 64.dp

    // Readable column width for screen content on wide windows.
    val ContentMaxWidth = 600.dp

    // Stroke widths
    val StrokeThin = 1.dp
    val StrokeBase = 1.5.dp
    val StrokeBold = 2.dp

    // Direct palette access for the moments a screen needs a token outside
    // Material's color roles (typically the warm coral accent).
    val Primary = Palette.Primary
    val PrimaryDark = Palette.PrimaryDark
    val PrimaryLight = Palette.PrimaryLight
    val Accent = Palette.Accent
    val AccentLight = Palette.AccentLight
    val Text = Palette.Text
    val TextMuted = Palette.TextMuted
    val TextSubtle = Palette.TextSubtle
    val Outline = Palette.Outline
    val Background = Palette.Background
    val Surface = Palette.Surface
    val SurfaceDim = Palette.SurfaceDim
    val Warning = Palette.Warning

    // Motion — durations. Use Compose's spring(...) for press feedback at
    // call sites with stiffness = StiffnessMediumLow, dampingRatio = 0.85f.
    object Motion {
        const val DURATION_INSTANT_MS = 100
        const val DURATION_QUICK_MS = 180
        const val DURATION_STANDARD_MS = 250
        const val DURATION_SLOW_MS = 400
    }
}
