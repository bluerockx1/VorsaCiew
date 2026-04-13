package com.vorsaciew.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette — pixel-inverted from the original CorsaView iOS gradient
// #c24181 (194,65,129)  → inverted → (61,190,126)  = #3DBE7E  (mint green)
// #5038ea (80,56,234)   → inverted → (175,199,21)  = #AFC715  (lime/chartreuse)
private val MintGreen   = Color(0xFF3DBE7E)
private val Chartreuse  = Color(0xFFAFC715)

private val LightColors = lightColorScheme(
    primary            = MintGreen,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFBEF5D8),
    onPrimaryContainer = Color(0xFF002116),
    secondary          = Color(0xFF6B7E00),   // dark chartreuse for readable text
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFE8F5A0),
    onSecondaryContainer = Color(0xFF1F2400),
    tertiary           = Color(0xFF006874),
    onTertiary         = Color.White,
    tertiaryContainer  = Color(0xFF97F0FF),
    onTertiaryContainer = Color(0xFF001F24),
    background         = Color(0xFFF6FFF4),
    onBackground       = Color(0xFF1A1C19),
    surface            = Color(0xFFF6FFF4),
    onSurface          = Color(0xFF1A1C19),
    surfaceVariant     = Color(0xFFDCE5D9),
    onSurfaceVariant   = Color(0xFF414941),
    error              = Color(0xFFBA1A1A),
    outline            = Color(0xFF717970),
)

private val DarkColors = darkColorScheme(
    primary            = Color(0xFF63DFA3),   // bright mint on dark
    onPrimary          = Color(0xFF003826),
    primaryContainer   = Color(0xFF005138),
    onPrimaryContainer = Color(0xFFBEF5D8),
    secondary          = Chartreuse,           // lime pops on dark bg
    onSecondary        = Color(0xFF313500),
    secondaryContainer = Color(0xFF494D00),
    onSecondaryContainer = Color(0xFFE8F5A0),
    tertiary           = Color(0xFF4FD8EB),
    onTertiary         = Color(0xFF00363D),
    tertiaryContainer  = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF97F0FF),
    background         = Color(0xFF0D1410),
    onBackground       = Color(0xFFE2E3DC),
    surface            = Color(0xFF1A1C19),
    onSurface          = Color(0xFFE2E3DC),
    surfaceVariant     = Color(0xFF232920),
    onSurfaceVariant   = Color(0xFFC1C9BF),
    error              = Color(0xFFFFB4AB),
    outline            = Color(0xFF8B938A),
)

@Composable
fun VorsaCiewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = VorsaCiewTypography,
        content     = content
    )
}
