package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EarthyDarkPrimary,
    secondary = EarthyDarkSecondary,
    tertiary = EarthyDarkTertiary,
    background = EarthyDarkBackground,
    surface = EarthyDarkSurface,
    onPrimary = EarthyDarkOnPrimary,
    onSecondary = EarthyDarkOnSecondary,
    onBackground = EarthyDarkOnBackground,
    onSurface = EarthyDarkOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EarthyPrimary,
    secondary = EarthySecondary,
    tertiary = EarthyTertiary,
    background = EarthyBackground,
    surface = EarthySurface,
    onPrimary = EarthyOnPrimary,
    onSecondary = EarthyOnSecondary,
    onBackground = EarthyOnBackground,
    onSurface = EarthyOnSurface
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set default dynamicColor to false to preserve the coffee branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
