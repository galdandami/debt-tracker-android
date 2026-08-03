package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class DebtColors(
    val owedToMe: Color,
    val owedToMeContainer: Color,
    val onOwedToMeContainer: Color,
    val iOwe: Color,
    val iOweContainer: Color,
    val onIOweContainer: Color
)

val LocalDebtColors = staticCompositionLocalOf {
    DebtColors(
        owedToMe = OwedToMeLight,
        owedToMeContainer = OwedToMeContainerLight,
        onOwedToMeContainer = OnOwedToMeContainerLight,
        iOwe = IOweLight,
        iOweContainer = IOweContainerLight,
        onIOweContainer = OnIOweContainerLight
    )
}

val MaterialTheme.debtColors: DebtColors
    @Composable
    @ReadOnlyComposable
    get() = LocalDebtColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun DebtTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val debtColors = if (darkTheme) {
        DebtColors(
            owedToMe = OwedToMeDark,
            owedToMeContainer = OwedToMeContainerDark,
            onOwedToMeContainer = OnOwedToMeContainerDark,
            iOwe = IOweDark,
            iOweContainer = IOweContainerDark,
            onIOweContainer = OnIOweContainerDark
        )
    } else {
        DebtColors(
            owedToMe = OwedToMeLight,
            owedToMeContainer = OwedToMeContainerLight,
            onOwedToMeContainer = OnOwedToMeContainerLight,
            iOwe = IOweLight,
            iOweContainer = IOweContainerLight,
            onIOweContainer = OnIOweContainerLight
        )
    }

    CompositionLocalProvider(LocalDebtColors provides debtColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
