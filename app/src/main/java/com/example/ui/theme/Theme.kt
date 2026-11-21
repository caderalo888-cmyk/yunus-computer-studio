package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioTeal,
    onPrimary = Slate950,
    primaryContainer = StudioTealContainerDark,
    onPrimaryContainer = StudioTealContainerLight,
    secondary = StudioAmber,
    onSecondary = Slate950,
    secondaryContainer = StudioAmberContainerDark,
    onSecondaryContainer = StudioAmberContainerLight,
    tertiary = StudioIndigo,
    onTertiary = Slate950,
    tertiaryContainer = StudioIndigoContainerDark,
    onTertiaryContainer = StudioIndigoContainerLight,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    error = StudioRose,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = StudioTealDark,
    onPrimary = Color.White,
    primaryContainer = StudioTealContainerLight,
    onPrimaryContainer = StudioTealContainerDark,
    secondary = StudioAmberDark,
    onSecondary = Color.White,
    secondaryContainer = StudioAmberContainerLight,
    onSecondaryContainer = StudioAmberContainerDark,
    tertiary = StudioIndigoDark,
    onTertiary = Color.White,
    tertiaryContainer = StudioIndigoContainerLight,
    onTertiaryContainer = StudioIndigoContainerDark,
    background = Paper50,
    onBackground = Slate900,
    surface = PaperWhite,
    onSurface = Slate900,
    surfaceVariant = Paper100,
    onSurfaceVariant = Slate600,
    outline = Paper200,
    error = StudioRose,
    onError = Color.White
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YunusStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
