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
    primary = ElegantBlueAccent,
    secondary = ElegantBlueLight,
    tertiary = ElegantYellowAccent,
    background = ElegantDarkBg,
    surface = ElegantDarkSurface,
    onBackground = TextNeutralLight,
    onSurface = TextNeutralLight
  )

private val LightColorScheme =
  darkColorScheme( // Handled as dark always since elegant cinema apps are best in dark mode
    primary = ElegantBlueAccent,
    secondary = ElegantBlueLight,
    tertiary = ElegantYellowAccent,
    background = ElegantDarkBg,
    surface = ElegantDarkSurface,
    onBackground = TextNeutralLight,
    onSurface = TextNeutralLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for cinema experience
  dynamicColor: Boolean = false, // Use our gorgeous custom StreamIT palette
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
