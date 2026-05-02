package ch.wuethrich.greenprint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenOnPrimary = Color(0xFFFFFFFF)
private val GreenPrimaryContainer = Color(0xFFA5D6A7)
private val GreenOnPrimaryContainer = Color(0xFF003300)
private val GreenSecondary = Color(0xFF558B2F)
private val GreenBackground = Color(0xFFF1F8E9)
private val GreenSurface = Color(0xFFFFFFFF)
private val GreenOnBackground = Color(0xFF1B1C18)
private val GreenOnSurface = Color(0xFF1B1C18)

private val GreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    background = GreenBackground,
    surface = GreenSurface,
    onBackground = GreenOnBackground,
    onSurface = GreenOnSurface,
)

@Composable
fun GreenPrintTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GreenColorScheme,
        content = content,
    )
}
