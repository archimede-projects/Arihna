package com.archimedeprojects.arihna.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ArihnaAlbaColors = lightColorScheme(
    primary = ArihnaGreen,
    onPrimary = ArihnaCream,
    secondary = ArihnaDawnGold,
    onSecondary = ArihnaForest,
    background = ArihnaDawnTop,
    onBackground = ArihnaForest,
    surface = ArihnaCream,
    onSurface = ArihnaForest,
    surfaceVariant = ArihnaSage,
    onSurfaceVariant = ArihnaMutedText,
    outline = ArihnaWarmOutline,
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun ArihnaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ArihnaAlbaColors,
        typography = ArihnaTypography,
        shapes = ArihnaShapes,
        content = content,
    )
}
