package com.ezmenu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable


private val LightScheme = lightColorScheme(
    primary = Color(0xFF98502F),
    onPrimary = EzColors.OnPrimary,
    primaryContainer = Color(0xFFF1E2D4),
    onPrimaryContainer = Color(0xFF67371F),
    inversePrimary = EzColors.InversePrimary,

    secondary = EzColors.Secondary,
    onSecondary = EzColors.OnSecondary,
    secondaryContainer = Color(0xFFDDE9DE),
    onSecondaryContainer = Color(0xFF245440),

    tertiary = EzColors.Tertiary,
    onTertiary = EzColors.OnTertiary,
    tertiaryContainer = EzColors.TertiaryContainer,
    onTertiaryContainer = EzColors.OnTertiaryContainer,

    background = Color(0xFFF9F6F0),
    onBackground = Color(0xFF2C2B27),

    surface = Color(0xFFF9F6F0),
    onSurface = Color(0xFF2C2B27),
    surfaceVariant = EzColors.SurfaceVariant,
    onSurfaceVariant = Color(0xFF777066),
    surfaceTint = EzColors.SurfaceTint,
    surfaceBright = EzColors.SurfaceBright,
    surfaceDim = EzColors.SurfaceDim,
    surfaceContainer = Color(0xFFEEE7DD),
    surfaceContainerHigh = Color(0xFFE8DFD2),
    surfaceContainerHighest = EzColors.SurfaceContainerHighest,
    surfaceContainerLow = Color(0xFFF2EDE5),
    surfaceContainerLowest = Color(0xFFFFFDF9),

    inverseSurface = EzColors.InverseSurface,
    inverseOnSurface = EzColors.InverseOnSurface,

    error = EzColors.Error,
    onError = EzColors.OnError,
    errorContainer = EzColors.ErrorContainer,
    onErrorContainer = EzColors.OnErrorContainer,

    outline = Color(0xFF9B9184),
    outlineVariant = Color(0xFFDDD4C7),
)


@Composable
fun EzMenuTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val view=androidx.compose.ui.platform.LocalView.current
    val context=androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            androidx.core.view.WindowCompat.getInsetsController(activity.window,view).apply {
                isAppearanceLightStatusBars=!dark
                isAppearanceLightNavigationBars=!dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
