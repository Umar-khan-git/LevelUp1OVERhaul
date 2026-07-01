package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
  lightColorScheme(
    primary = Accent,
    secondary = AccentEnd,
    tertiary = AccentEnd,
    background = CanvasBg,
    surface = LayerCard,
    onBackground = PrimaryText,
    onSurface = PrimaryText,
    primaryContainer = Accent,
    onPrimaryContainer = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Bright light theme
  dynamicColor: Boolean = false, // Disable dynamic color to keep the Sunset accent
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  // Dark status-bar icons so the clock/battery stay visible on the light background.
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
