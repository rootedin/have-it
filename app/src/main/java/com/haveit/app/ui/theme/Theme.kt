package com.haveit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HaveItDarkColorScheme = darkColorScheme(
    primary = Coral500,
    onPrimary = Color.White,
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = CoralOnContainerDark,
    secondary = Coral300,
    onSecondary = Color(0xFF3A1810),
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = TextOnDark,
    tertiary = AccentBlue,
    onTertiary = Color(0xFF0E2A47),
    tertiaryContainer = AccentContainerDark,
    onTertiaryContainer = AccentOnDark,
    background = DarkBg,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = TextOnDarkDim,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = MissedRed,
    onError = Color(0xFF3B111A),
)

private val HaveItLightColorScheme = lightColorScheme(
    primary = Coral600,
    onPrimary = Color.White,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = CoralOnContainerLight,
    secondary = Coral600,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceHigh,
    onSecondaryContainer = TextOnLight,
    tertiary = AccentBlue,
    onTertiary = Color.White,
    tertiaryContainer = AccentContainerLight,
    onTertiaryContainer = AccentOnLight,
    background = LightBg,
    onBackground = TextOnLight,
    surface = Color.White,
    onSurface = TextOnLight,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = TextOnLightDim,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = MissedRed,
    onError = Color.White,
)

private val HaveItShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HaveItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) HaveItDarkColorScheme else HaveItLightColorScheme,
        typography = HaveItTypography,
        shapes = HaveItShapes,
        content = content,
    )
}
