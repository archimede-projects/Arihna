package com.archimedeprojects.arihna.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = ArihnaGreen,
    onPrimary = ArihnaOffWhite,
    secondary = ArihnaGold,
    onSecondary = ArihnaDarkGreen,
    background = ArihnaOffWhite,
    onBackground = ArihnaDarkGreen,
    surface = ArihnaOffWhite,
    onSurface = ArihnaDarkGreen,
    outline = ArihnaMutedGreen,
)

private val DarkColors = darkColorScheme(
    primary = ArihnaSoftGold,
    onPrimary = ArihnaDarkGreen,
    secondary = ArihnaGold,
    onSecondary = ArihnaDarkGreen,
    background = ArihnaDarkGreen,
    onBackground = ArihnaOffWhite,
    surface = ArihnaDeepSurface,
    onSurface = ArihnaOffWhite,
    outline = ArihnaMutedGreen,
)

@Composable
fun ArihnaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ArihnaTypography,
        shapes = ArihnaShapes,
        content = content,
    )
}
