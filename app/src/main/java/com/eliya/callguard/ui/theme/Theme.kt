package com.eliya.callguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A quiet, classic palette: deep navy ink, warm paper, a touch of bronze.
private val Light = lightColorScheme(
    primary = Color(0xFF1E3A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5F1),
    onPrimaryContainer = Color(0xFF0E1F33),
    secondary = Color(0xFF8A6D3B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E8D6),
    onSecondaryContainer = Color(0xFF2C2210),
    tertiary = Color(0xFF4F6B5A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCEADF),
    onTertiaryContainer = Color(0xFF13241A),
    error = Color(0xFFA8322D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DEDB),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFBF9F6),
    onBackground = Color(0xFF1B1C1E),
    surface = Color(0xFFFBF9F6),
    onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE9E5DF),
    onSurfaceVariant = Color(0xFF5A5E66),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F3EE),
    surfaceContainer = Color(0xFFF1EDE7),
    surfaceContainerHigh = Color(0xFFEBE7E1),
    surfaceContainerHighest = Color(0xFFE5E1DA),
    outline = Color(0xFF8C9099),
    outlineVariant = Color(0xFFDAD5CD),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFAFC8E8),
    onPrimary = Color(0xFF0B2440),
    primaryContainer = Color(0xFF243F5F),
    onPrimaryContainer = Color(0xFFD8E4F4),
    secondary = Color(0xFFDCC59C),
    onSecondary = Color(0xFF3D2F14),
    secondaryContainer = Color(0xFF4C3D22),
    onSecondaryContainer = Color(0xFFF4E6CC),
    tertiary = Color(0xFFB2CDBA),
    onTertiary = Color(0xFF1E3527),
    tertiaryContainer = Color(0xFF354C3E),
    onTertiaryContainer = Color(0xFFD0E8D6),
    error = Color(0xFFF2B8B2),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7D2420),
    onErrorContainer = Color(0xFFF9DEDA),
    background = Color(0xFF121417),
    onBackground = Color(0xFFE4E2DE),
    surface = Color(0xFF121417),
    onSurface = Color(0xFFE4E2DE),
    surfaceVariant = Color(0xFF2B2E33),
    onSurfaceVariant = Color(0xFFC2C6CE),
    surfaceContainerLowest = Color(0xFF0D0F12),
    surfaceContainerLow = Color(0xFF1A1C20),
    surfaceContainer = Color(0xFF1E2125),
    surfaceContainerHigh = Color(0xFF282B30),
    surfaceContainerHighest = Color(0xFF33363B),
    outline = Color(0xFF8C9099),
    outlineVariant = Color(0xFF3A3E44),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Accent colors for each filtering mode, so the three are recognizable at a glance. */
@Immutable
data class ModeColors(
    val blockContainer: Color,
    val onBlockContainer: Color,
    val ignoreContainer: Color,
    val onIgnoreContainer: Color,
    val hangUpContainer: Color,
    val onHangUpContainer: Color,
)

val LocalModeColors = staticCompositionLocalOf {
    ModeColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

@Composable
fun CallGuardTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (dark) Dark else Light
    val modes = ModeColors(
        blockContainer = scheme.errorContainer,
        onBlockContainer = scheme.onErrorContainer,
        ignoreContainer = scheme.primaryContainer,
        onIgnoreContainer = scheme.onPrimaryContainer,
        hangUpContainer = scheme.secondaryContainer,
        onHangUpContainer = scheme.onSecondaryContainer,
    )
    CompositionLocalProvider(LocalModeColors provides modes) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
