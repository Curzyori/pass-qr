package com.passqr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary            = CoralPrimary,
    onPrimary          = OnPrimary,
    primaryContainer   = CoralLight,
    onPrimaryContainer = InkDeep,

    secondary          = SurfaceDark,
    onSecondary        = OnSurface,

    background         = CanvasCream,
    onBackground       = OnCanvas,

    surface            = CanvasCream,
    onSurface          = OnCanvas,

    surfaceVariant     = SurfaceElevated,
    onSurfaceVariant   = OnSurfaceElevated,

    outline            = DividerColor,
    outlineVariant     = DividerColor,

    error              = ErrorRed,
    onError            = OnPrimary
)

@Composable
fun PassQRTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = PassQRTypography,
        content     = content
    )
}
