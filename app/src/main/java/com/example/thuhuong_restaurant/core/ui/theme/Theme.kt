package com.example.thuhuong_restaurant.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ThPrimary,
    onPrimary = ThOnPrimary,
    primaryContainer = ThSurfaceCard,
    onPrimaryContainer = ThInk,
    secondary = ThPrimaryDark,
    onSecondary = ThOnPrimary,
    background = ThCanvas,
    onBackground = ThInk,
    surface = ThCanvas,
    onSurface = ThInk,
    surfaceVariant = ThSurfaceCard,
    onSurfaceVariant = ThMuted,
    outline = ThHairline,
    outlineVariant = ThHairline,
    error = ThError,
    onError = ThOnPrimary,
)

private val DarkColors = darkColorScheme(
    primary = ThPrimary,
    onPrimary = ThOnPrimary,
    primaryContainer = ThSurfaceDarkElevated,
    onPrimaryContainer = ThOnDark,
    secondary = ThPrimaryDark,
    onSecondary = ThOnPrimary,
    background = ThSurfaceDark,
    onBackground = ThOnDark,
    surface = ThSurfaceDark,
    onSurface = ThOnDark,
    surfaceVariant = ThSurfaceDarkElevated,
    onSurfaceVariant = ThOnDarkSoft,
    outline = ThSurfaceDarkElevated,
    outlineVariant = ThSurfaceDarkElevated,
    error = ThError,
    onError = ThOnPrimary,
)

// rounded-xl (cards/images) / rounded-lg (buttons) / sharp (filter pills use RectangleShape directly)
private val ThShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ThuHuongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ThTypography,
        shapes = ThShapes,
        content = content,
    )
}
