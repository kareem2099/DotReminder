package com.FreeRave.remindme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import com.FreeRave.remindme.data.ThemeSetting

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDBE5DE),
    error = LightError,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onTertiary = LightOnTertiary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF404943),
    onError = LightOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF707973),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFBFC9C2)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C322E),
    error = DarkError,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onTertiary = DarkOnTertiary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
    onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF8A938C),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF404943)
)

private val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanBlueLightPrimary,
    secondary = OceanBlueLightSecondary,
    tertiary = OceanBlueLightTertiary,
    background = OceanBlueLightBackground,
    surface = OceanBlueLightSurface,
    surfaceVariant = OceanBlueLightSurfaceVariant,
    error = OceanBlueLightError,
    onPrimary = OceanBlueLightOnPrimary,
    onSecondary = OceanBlueLightOnSecondary,
    onTertiary = OceanBlueLightOnTertiary,
    onBackground = OceanBlueLightOnBackground,
    onSurface = OceanBlueLightOnSurface,
    onSurfaceVariant = OceanBlueLightOnSurfaceVariant,
    onError = OceanBlueLightOnError,
    outline = OceanBlueLightOutline,
    outlineVariant = OceanBlueLightOutlineVariant
)

private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanBlueDarkPrimary,
    secondary = OceanBlueDarkSecondary,
    tertiary = OceanBlueDarkTertiary,
    background = OceanBlueDarkBackground,
    surface = OceanBlueDarkSurface,
    surfaceVariant = OceanBlueDarkSurfaceVariant,
    error = OceanBlueDarkError,
    onPrimary = OceanBlueDarkOnPrimary,
    onSecondary = OceanBlueDarkOnSecondary,
    onTertiary = OceanBlueDarkOnTertiary,
    onBackground = OceanBlueDarkOnBackground,
    onSurface = OceanBlueDarkOnSurface,
    onSurfaceVariant = OceanBlueDarkOnSurfaceVariant,
    onError = OceanBlueDarkOnError,
    outline = OceanBlueDarkOutline,
    outlineVariant = OceanBlueDarkOutlineVariant
)

private val ForestGreenLightColorScheme = lightColorScheme(
    primary = ForestGreenLightPrimary,
    secondary = ForestGreenLightSecondary,
    tertiary = ForestGreenLightTertiary,
    background = ForestGreenLightBackground,
    surface = ForestGreenLightSurface,
    surfaceVariant = ForestGreenLightSurfaceVariant,
    error = ForestGreenLightError,
    onPrimary = ForestGreenLightOnPrimary,
    onSecondary = ForestGreenLightOnSecondary,
    onTertiary = ForestGreenLightOnTertiary,
    onBackground = ForestGreenLightOnBackground,
    onSurface = ForestGreenLightOnSurface,
    onSurfaceVariant = ForestGreenLightOnSurfaceVariant,
    onError = ForestGreenLightOnError,
    outline = ForestGreenLightOutline,
    outlineVariant = ForestGreenLightOutlineVariant
)

private val ForestGreenDarkColorScheme = darkColorScheme(
    primary = ForestGreenDarkPrimary,
    secondary = ForestGreenDarkSecondary,
    tertiary = ForestGreenDarkTertiary,
    background = ForestGreenDarkBackground,
    surface = ForestGreenDarkSurface,
    surfaceVariant = ForestGreenDarkSurfaceVariant,
    error = ForestGreenDarkError,
    onPrimary = ForestGreenDarkOnPrimary,
    onSecondary = ForestGreenDarkOnSecondary,
    onTertiary = ForestGreenDarkOnTertiary,
    onBackground = ForestGreenDarkOnBackground,
    onSurface = ForestGreenDarkOnSurface,
    onSurfaceVariant = ForestGreenDarkOnSurfaceVariant,
    onError = ForestGreenDarkOnError,
    outline = ForestGreenDarkOutline,
    outlineVariant = ForestGreenDarkOutlineVariant
)

private val SunsetOrangeLightColorScheme = lightColorScheme(
    primary = SunsetOrangeLightPrimary,
    secondary = SunsetOrangeLightSecondary,
    tertiary = SunsetOrangeLightTertiary,
    background = SunsetOrangeLightBackground,
    surface = SunsetOrangeLightSurface,
    surfaceVariant = SunsetOrangeLightSurfaceVariant,
    error = SunsetOrangeLightError,
    onPrimary = SunsetOrangeLightOnPrimary,
    onSecondary = SunsetOrangeLightOnSecondary,
    onTertiary = SunsetOrangeLightOnTertiary,
    onBackground = SunsetOrangeLightOnBackground,
    onSurface = SunsetOrangeLightOnSurface,
    onSurfaceVariant = SunsetOrangeLightOnSurfaceVariant,
    onError = SunsetOrangeLightOnError,
    outline = SunsetOrangeLightOutline,
    outlineVariant = SunsetOrangeLightOutlineVariant
)

private val SunsetOrangeDarkColorScheme = darkColorScheme(
    primary = SunsetOrangeDarkPrimary,
    secondary = SunsetOrangeDarkSecondary,
    tertiary = SunsetOrangeDarkTertiary,
    background = SunsetOrangeDarkBackground,
    surface = SunsetOrangeDarkSurface,
    surfaceVariant = SunsetOrangeDarkSurfaceVariant,
    error = SunsetOrangeDarkError,
    onPrimary = SunsetOrangeDarkOnPrimary,
    onSecondary = SunsetOrangeDarkOnSecondary,
    onTertiary = SunsetOrangeDarkOnTertiary,
    onBackground = SunsetOrangeDarkOnBackground,
    onSurface = SunsetOrangeDarkOnSurface,
    onSurfaceVariant = SunsetOrangeDarkOnSurfaceVariant,
    onError = SunsetOrangeDarkOnError,
    outline = SunsetOrangeDarkOutline,
    outlineVariant = SunsetOrangeDarkOutlineVariant
)

private val MidnightPurpleLightColorScheme = lightColorScheme(
    primary = MidnightPurpleLightPrimary,
    secondary = MidnightPurpleLightSecondary,
    tertiary = MidnightPurpleLightTertiary,
    background = MidnightPurpleLightBackground,
    surface = MidnightPurpleLightSurface,
    surfaceVariant = MidnightPurpleLightSurfaceVariant,
    error = MidnightPurpleLightError,
    onPrimary = MidnightPurpleLightOnPrimary,
    onSecondary = MidnightPurpleLightOnSecondary,
    onTertiary = MidnightPurpleLightOnTertiary,
    onBackground = MidnightPurpleLightOnBackground,
    onSurface = MidnightPurpleLightOnSurface,
    onSurfaceVariant = MidnightPurpleLightOnSurfaceVariant,
    onError = MidnightPurpleLightOnError,
    outline = MidnightPurpleLightOutline,
    outlineVariant = MidnightPurpleLightOutlineVariant
)

private val MidnightPurpleDarkColorScheme = darkColorScheme(
    primary = MidnightPurpleDarkPrimary,
    secondary = MidnightPurpleDarkSecondary,
    tertiary = MidnightPurpleDarkTertiary,
    background = MidnightPurpleDarkBackground,
    surface = MidnightPurpleDarkSurface,
    surfaceVariant = MidnightPurpleDarkSurfaceVariant,
    error = MidnightPurpleDarkError,
    onPrimary = MidnightPurpleDarkOnPrimary,
    onSecondary = MidnightPurpleDarkOnSecondary,
    onTertiary = MidnightPurpleDarkOnTertiary,
    onBackground = MidnightPurpleDarkOnBackground,
    onSurface = MidnightPurpleDarkOnSurface,
    onSurfaceVariant = MidnightPurpleDarkOnSurfaceVariant,
    onError = MidnightPurpleDarkOnError,
    outline = MidnightPurpleDarkOutline,
    outlineVariant = MidnightPurpleDarkOutlineVariant
)

private val CandyLightColorScheme = lightColorScheme(
    primary = CandyLightPrimary,
    secondary = CandyLightSecondary,
    tertiary = CandyLightTertiary,
    background = CandyLightBackground,
    surface = CandyLightSurface,
    surfaceVariant = CandyLightSurfaceVariant,
    error = CandyLightError,
    onPrimary = CandyLightOnPrimary,
    onSecondary = CandyLightOnSecondary,
    onTertiary = CandyLightOnTertiary,
    onBackground = CandyLightOnBackground,
    onSurface = CandyLightOnSurface,
    onSurfaceVariant = CandyLightOnSurfaceVariant,
    onError = CandyLightOnError,
    outline = CandyLightOutline,
    outlineVariant = CandyLightOutlineVariant
)

private val CandyDarkColorScheme = darkColorScheme(
    primary = CandyDarkPrimary,
    secondary = CandyDarkSecondary,
    tertiary = CandyDarkTertiary,
    background = CandyDarkBackground,
    surface = CandyDarkSurface,
    surfaceVariant = CandyDarkSurfaceVariant,
    error = CandyDarkError,
    onPrimary = CandyDarkOnPrimary,
    onSecondary = CandyDarkOnSecondary,
    onTertiary = CandyDarkOnTertiary,
    onBackground = CandyDarkOnBackground,
    onSurface = CandyDarkOnSurface,
    onSurfaceVariant = CandyDarkOnSurfaceVariant,
    onError = CandyDarkOnError,
    outline = CandyDarkOutline,
    outlineVariant = CandyDarkOutlineVariant
)

@Composable
fun RemindMeAppTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeSetting) {
        ThemeSetting.LIGHT -> LightColorScheme
        ThemeSetting.DARK -> DarkColorScheme
        ThemeSetting.SYSTEM -> if (useDarkTheme) DarkColorScheme else LightColorScheme
        ThemeSetting.OCEAN_BLUE -> if (useDarkTheme) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
        ThemeSetting.FOREST_GREEN -> if (useDarkTheme) ForestGreenDarkColorScheme else ForestGreenLightColorScheme
        ThemeSetting.SUNSET_ORANGE -> if (useDarkTheme) SunsetOrangeDarkColorScheme else SunsetOrangeLightColorScheme
        ThemeSetting.MIDNIGHT_PURPLE -> if (useDarkTheme) MidnightPurpleDarkColorScheme else MidnightPurpleLightColorScheme
        ThemeSetting.CANDY -> if (useDarkTheme) CandyDarkColorScheme else CandyLightColorScheme
    }

    val activity = LocalActivity.current
    val view = LocalView.current
    if (!view.isInEditMode && activity != null) SideEffect {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
            isAppearanceLightNavigationBars = colorScheme.background.luminance() > 0.5f
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
